import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.compose.ui.text.drawText
import androidx.core.text.color

class CustomDrawView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 8f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // Draw Background Shape
        paint.color = Color.LTGRAY
        canvas.drawRect(50f, 50f, width - 50f, 200f, paint)

        // Draw Stylized Text
        paint.color = Color.BLUE
        paint.textSize = 60f
        canvas.drawText("TEAM SAFETY SHIELD", 100f, 150f, paint)

        // Draw Overlapping Circle
        paint.color = Color.RED
        paint.style = Paint.Style.STROKE
        canvas.drawCircle(width / 2f, 400f, 100f, paint)
    }
}