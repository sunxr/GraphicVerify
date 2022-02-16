package com.sun.alone.mylibrary

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.*

class GraphicVerifyView @JvmOverloads constructor(
  context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
  private lateinit var bitmap: Bitmap
  private lateinit var paint: Paint
  private val pathSeekBg: Path = Path() //滑块背景路径
  private var currentStatus = Status.DEFAULT //当前的验证状态
  private var circleRadius = 0f //滑块两边圆角的半径
  private var isTouch = false //是否触摸滑块
  private var seekTop = 0f //滑块距离顶部的距离
  private var seekMoveX = 0f //手指触摸的X轴位置
  private var seekCenterX = 0f //滑块的中心点位置
  private var imgSrc = R.drawable.ic_img //展示的图片
  private var defaultDegree = 0f // 默认角度、初始角度
  private var currentDegrees = 0f //当前的图片角度
  var seekBorderWidth = 4f // 滑块描边宽度
  var offsetDegrees = 10f // 允许的误差角度
  var verifyCallBack: VerifyCallBack? = null //验证回调
  var seekBgColor = Color.parseColor("#EEEEEE") // 滑块背景颜色
  var seekDefaultColor = Color.WHITE // 滑块默认颜色
  var seekBorderColor = Color.GRAY // 滑块描边颜色
  var seekTouchColor = Color.BLUE // 滑块触摸的颜色
  var seekVerFailColor = Color.RED // 滑块验证失败的颜色
  var seekVerSuccessColor = Color.GREEN // 滑块验证成功的颜色
  var seekArrowDefaultColor = Color.GRAY // 滑块箭头默认颜色
  var seekArrowTouchColor = Color.WHITE // 滑块箭头触摸颜色

  init {
    initAttr(attrs)
    init()
  }

  private fun initAttr(attrs: AttributeSet?) {
    attrs?.let {
      val typeStyle = context.obtainStyledAttributes(attrs, R.styleable.GraphicsVerifyView)
      seekBorderWidth = typeStyle.getDimension(R.styleable.GraphicsVerifyView_seekBorderWidth, 4f)
      offsetDegrees = typeStyle.getFloat(R.styleable.GraphicsVerifyView_offsetDegrees,10f) //允许的误差角度
      seekBgColor = typeStyle.getColor(R.styleable.GraphicsVerifyView_seekBgColor,Color.parseColor("#EEEEEE")) //滑块背景颜色
      seekDefaultColor = typeStyle.getColor(R.styleable.GraphicsVerifyView_seekDefaultColor,Color.WHITE) //滑块默认颜色
      seekBorderColor = typeStyle.getColor(R.styleable.GraphicsVerifyView_seekBorderColor,Color.GRAY) //滑块描边颜色
      seekTouchColor = typeStyle.getColor(R.styleable.GraphicsVerifyView_seekTouchColor,Color.BLUE) //滑块触摸的颜色
      seekVerFailColor = typeStyle.getColor(R.styleable.GraphicsVerifyView_seekVerFailColor,Color.RED) //滑块验证失败的颜色
      seekVerSuccessColor = typeStyle.getColor(R.styleable.GraphicsVerifyView_seekVerSuccessColor,Color.GREEN) //滑块验证成功的颜色
      seekArrowDefaultColor = typeStyle.getColor(R.styleable.GraphicsVerifyView_seekArrowDefaultColor,Color.GRAY) //滑块箭头默认颜色
      seekArrowTouchColor = typeStyle.getColor(R.styleable.GraphicsVerifyView_seekArrowTouchColor,Color.WHITE) //滑块箭头触摸颜色
      imgSrc = typeStyle.getResourceId(R.styleable.GraphicsVerifyView_imgSrc,R.drawable.ic_img)
      typeStyle.recycle()
    }
  }

  private fun init() {
    // 关闭硬件加速
    setLayerType(LAYER_TYPE_SOFTWARE, null)
    bitmap = BitmapFactory.decodeResource(context.resources, imgSrc)
    paint = Paint(Paint.ANTI_ALIAS_FLAG)
    //随机初始化默认角度（-80～-280）,匹配时与滑块旋转的角度相加如果在误差范围内就验证成功
    val randomValue = Random().nextInt(201) + 80f;
    defaultDegree = -randomValue
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val width = measureWidth(widthMeasureSpec)
    setMeasuredDimension(width, measureHeight(width))
  }

  /**
   * 测量宽度
   * */
  private fun measureWidth(widthMeasureSpec: Int): Int {
    val mode = MeasureSpec.getMode(widthMeasureSpec)
    var width = MeasureSpec.getSize(widthMeasureSpec)
    if (MeasureSpec.AT_MOST == mode) {
      width = 300
    }
    return width
  }

  /**
   * 测量高度
   * */
  private fun measureHeight(width: Int): Int {
    return (width / 10f + width / 2f + width / 6f + seekBorderWidth / 2f).toInt()
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    // 缩放图片大小(宽高为控件高度的1/2)
    val scaleValue = w / 2
    bitmap = Bitmap.createScaledBitmap(bitmap, scaleValue, scaleValue, true)
    seekTop = width / 10f + bitmap.width
    // 滑块两边圆角的半径为控件宽度的十二分之一
    circleRadius = width / 12f
    initSeekBgPath()
  }

  private fun initSeekBgPath() {
    // 距离上边图片的高度
    val top = seekTop
    // 由于画笔的stroke是平均向内向外扩散的，因此需要滑块背景两边需要预留二分之一的seekBgBorderWidth的宽度才能保证不超出控件
    val borderOffset = seekBorderWidth / 2
    // 通过计算得出滑块背景的路径
    pathSeekBg.moveTo(circleRadius + borderOffset, top)
    pathSeekBg.addArc(borderOffset, top, circleRadius * 2 + borderOffset, top + circleRadius * 2, -90f, -180f)
    pathSeekBg.lineTo(width - circleRadius - borderOffset, top + circleRadius * 2)
    pathSeekBg.addArc(width - circleRadius * 2 - borderOffset, top, width - borderOffset, top + circleRadius * 2, 90f, -180f)
    pathSeekBg.lineTo(circleRadius + borderOffset, top)
  }

  override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)
    canvas?.let {
      drawSeekBg(it)
      drawSeek(it)
      drawCircleImage(it)
    }
  }

  /**
   * 画滑块背景
   * */
  private fun drawSeekBg(canvas: Canvas) {
    //先画填充色
    paint.color = seekBgColor
    paint.strokeWidth = seekBorderWidth
    paint.style = Paint.Style.FILL
    canvas.drawPath(pathSeekBg, paint)
    //画边
    paint.color = seekBorderColor
    paint.style = Paint.Style.STROKE
    canvas.drawPath(pathSeekBg, paint)
  }

  /**
   * 画滑块
   * */
  private fun drawSeek(canvas: Canvas) {
    //圆的中心点X(根据手指位置动态改变)
    seekCenterX = when {
      //处理左边的边界值
      seekMoveX < circleRadius -> {
        circleRadius
      }
      //处理右边的边界值
      seekMoveX > width - circleRadius -> {
        width - circleRadius
      }
      else -> {
        seekMoveX
      }
    }
    //圆的中心点Y
    val centerY = seekTop + circleRadius
    paint.color = when {
      isTouch -> {
        seekTouchColor
      }
      currentStatus == Status.SUCCESS -> {
        seekVerSuccessColor
      }
      currentStatus == Status.FAIL -> {
        seekVerFailColor
      }
      else -> {
        seekDefaultColor
      }
    }
    paint.style = Paint.Style.FILL
    canvas.drawCircle(seekCenterX, centerY, circleRadius - seekBorderWidth, paint)
    paint.style = Paint.Style.STROKE
    paint.color = when {
      isTouch -> {
        seekTouchColor
      }
      currentStatus == Status.SUCCESS -> {
        seekVerSuccessColor
      }
      currentStatus == Status.FAIL -> {
        seekVerFailColor
      }
      else -> {
        seekBorderColor
      }
    }
    paint.strokeWidth = seekBorderWidth
    canvas.drawCircle(seekCenterX, centerY, circleRadius - seekBorderWidth, paint)

    //画滑块中间的两个箭头
    paint.textSize = circleRadius
    paint.color = when {
      isTouch -> {
        seekArrowTouchColor
      }
      currentStatus == Status.SUCCESS -> {
        seekArrowTouchColor
      }
      currentStatus == Status.FAIL -> {
        seekArrowTouchColor
      }
      else -> {
        seekArrowDefaultColor
      }
    }
    paint.style = Paint.Style.FILL_AND_STROKE
    paint.textAlign = Paint.Align.CENTER
    paint.strokeWidth = 2f
    val fontMetrics = paint.fontMetrics
    // 计算文字高度<<
    val fontHeight = fontMetrics.bottom - fontMetrics.top
    // 计算文字baseline，让文字垂直居中
    val textBaseY = (circleRadius * 2 - fontHeight) / 2

    canvas.save()
    canvas.translate(seekCenterX, centerY)
    canvas.rotate(180f)
    canvas.drawText("<<", 0f, textBaseY, paint)
    canvas.restore()
  }

  /**
   * 画圆形图片
   * */
  private fun drawCircleImage(canvas: Canvas) {
    //根据滑块移动的距离计算旋转的角度
    currentDegrees = (seekCenterX - circleRadius) / (width - circleRadius * 2) * 360 + defaultDegree
    canvas.save()
    //先将画布移动到原点为（width/2f, width/4f）的位置（即：显示图片的中心点位置）
    canvas.translate(width / 2f, width / 4f)
    //根据拖动滑块来调整角度
    canvas.rotate(currentDegrees)
    //利用混合模式将图片画成圆形
    canvas.drawCircle(0f, 0f, (width / 4).toFloat(), paint)
    paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
    canvas.drawBitmap(bitmap, -bitmap.width / 2f, -bitmap.width / 2f, paint)
    //清空混合模式
    paint.xfermode = null
    canvas.restore()
  }

  override fun onTouchEvent(event: MotionEvent?): Boolean {
    event?.let {
      when(event.action) {
        MotionEvent.ACTION_DOWN -> {
          //当状态为默认时才可以拖动
          if (currentStatus == Status.DEFAULT) {
            //判断触摸点是否在滑块上
            val rectF = RectF(0f, seekTop, circleRadius * 2, seekTop + circleRadius * 2)
            if (rectF.contains(event.x, event.y)) {
              isTouch = true
              postInvalidate()
            }
          }
        }
        MotionEvent.ACTION_MOVE -> {
          if (isTouch) {
            seekMoveX = event.x
            postInvalidate()
          }
        }
        MotionEvent.ACTION_UP -> {
          currentStatus = if (currentDegrees <= offsetDegrees && currentDegrees >= - offsetDegrees) {
            // 验证成功
            Status.SUCCESS
          }
          else {
            // 验证失败
            Status.FAIL
          }
          verifyCallBack?.let {
            if (currentStatus == Status.SUCCESS) {
              it.onSuccess()
            }
            else {
              it.onFail()
            }
          }
          isTouch = false
          postInvalidate()
        }
        else -> {}
      }
    }
    return isTouch
  }

  /**
   * 验证的三个状态
   */
  enum class Status{
    DEFAULT,//默认
    FAIL, //失败
    SUCCESS //成功
  }

  interface VerifyCallBack{
    fun onSuccess()
    fun onFail()
  }
}