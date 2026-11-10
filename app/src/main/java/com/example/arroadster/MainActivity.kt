package com.example.arroadster

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.max

class MainActivity : AppCompatActivity() {
    private lateinit var gameView: RoadsterGameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gameView = RoadsterGameView(this)
        setContentView(gameView)
    }

    override fun onPause() {
        gameView.stopGameLoop()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::gameView.isInitialized) gameView.startGameLoop()
    }
}

/** A lightweight, non-AR road-dodging game drawn with Android Canvas. */
private class RoadsterGameView(context: android.content.Context) : View(context) {
    private data class Obstacle(var lane: Int, var progress: Float)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val roadPath = Path()
    private val obstacles = mutableListOf<Obstacle>()
    private var playerLane = 0
    private var score = 0
    private var gameOver = false
    private var roadOffset = 0f
    private var spawnTimer = 0f
    private var lastFrameNanos = 0L
    private var running = false

    private val backgroundColor = Color.rgb(16, 24, 35)
    private val roadColor = Color.rgb(54, 59, 67)
    private val stripeColor = Color.rgb(245, 220, 120)
    private val playerColor = Color.rgb(42, 170, 255)
    private val obstacleColor = Color.rgb(244, 91, 67)

    fun startGameLoop() {
        if (!running) {
            running = true
            lastFrameNanos = 0L
            postInvalidateOnAnimation()
        }
    }

    fun stopGameLoop() {
        running = false
        removeCallbacks(invalidateRunnable)
    }

    private val invalidateRunnable = Runnable { invalidate() }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val dt = if (lastFrameNanos == 0L) 0f else
            ((System.nanoTime() - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastFrameNanos = System.nanoTime()
        if (running && !gameOver) updateGame(dt)

        drawScene(canvas)
        if (running) postOnAnimation(invalidateRunnable)
    }

    private fun updateGame(dt: Float) {
        roadOffset = (roadOffset + dt * 0.8f) % 1f
        spawnTimer += dt
        if (spawnTimer >= 1.25f) {
            spawnTimer = 0f
            val lane = (-1..1).random()
            obstacles.add(Obstacle(lane, 0f))
        }

        val iterator = obstacles.iterator()
        while (iterator.hasNext()) {
            val obstacle = iterator.next()
            obstacle.progress += dt * 0.27f
            if (obstacle.progress >= 0.80f && obstacle.lane == playerLane) {
                gameOver = true
            }
            if (obstacle.progress >= 1.08f) {
                iterator.remove()
                score += 1
            }
        }
    }

    private fun drawScene(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val horizon = h * 0.27f
        val bottom = h * 0.94f
        val center = w / 2f
        val topHalf = w * 0.075f
        val bottomHalf = w * 0.47f

        canvas.drawColor(backgroundColor)
        paint.color = Color.rgb(48, 105, 78)
        canvas.drawRect(0f, horizon, w, bottom, paint)

        roadPath.reset()
        roadPath.moveTo(center - topHalf, horizon)
        roadPath.lineTo(center + topHalf, horizon)
        roadPath.lineTo(center + bottomHalf, bottom)
        roadPath.lineTo(center - bottomHalf, bottom)
        roadPath.close()
        paint.color = roadColor
        canvas.drawPath(roadPath, paint)

        paint.color = Color.WHITE
        paint.strokeWidth = max(2f, w * 0.008f)
        canvas.drawLine(center - topHalf, horizon, center - bottomHalf, bottom, paint)
        canvas.drawLine(center + topHalf, horizon, center + bottomHalf, bottom, paint)

        // Dashed lane dividers move toward the player to create a perspective effect.
        for (divider in 1..2) {
            val topX = center - topHalf + (2f * topHalf / 3f) * divider
            val bottomX = center - bottomHalf + (2f * bottomHalf / 3f) * divider
            for (segment in 0..10) {
                val p1 = (segment / 11f + roadOffset) % 1f
                val p2 = (p1 + 0.045f).coerceAtMost(1f)
                if (p2 > p1) {
                    paint.color = stripeColor
                    paint.strokeWidth = max(2f, w * 0.009f * p1)
                    canvas.drawLine(
                        lerp(topX, bottomX, p1), lerp(horizon, bottom, p1),
                        lerp(topX, bottomX, p2), lerp(horizon, bottom, p2), paint
                    )
                }
            }
        }

        obstacles.forEach { drawCar(canvas, it.lane, it.progress, obstacleColor, horizon, bottom, center, topHalf, bottomHalf) }
        drawCar(canvas, playerLane, 0.82f, playerColor, horizon, bottom, center, topHalf, bottomHalf)
        drawHud(canvas, w, h)
    }

    private fun drawCar(
        canvas: Canvas, lane: Int, progress: Float, color: Int,
        horizon: Float, bottom: Float, center: Float, topHalf: Float, bottomHalf: Float
    ) {
        val p = progress.coerceIn(0f, 1f)
        val y = lerp(horizon, bottom, p)
        val roadHalf = lerp(topHalf, bottomHalf, p)
        val x = center + lane * roadHalf * (2f / 3f)
        val carWidth = lerp(width * 0.035f, width * 0.15f, p)
        val carHeight = carWidth * 1.45f

        paint.color = Color.BLACK
        canvas.drawRoundRect(x - carWidth * 0.57f, y - carHeight * 0.25f, x - carWidth * 0.36f, y + carHeight * 0.42f, 5f, 5f, paint)
        canvas.drawRoundRect(x + carWidth * 0.36f, y - carHeight * 0.25f, x + carWidth * 0.57f, y + carHeight * 0.42f, 5f, 5f, paint)
        paint.color = color
        canvas.drawRoundRect(x - carWidth / 2f, y - carHeight / 2f, x + carWidth / 2f, y + carHeight / 2f, carWidth * 0.16f, carWidth * 0.16f, paint)
        paint.color = Color.rgb(185, 225, 245)
        canvas.drawRoundRect(x - carWidth * 0.31f, y - carHeight * 0.34f, x + carWidth * 0.31f, y - carHeight * 0.06f, carWidth * 0.08f, carWidth * 0.08f, paint)
    }

    private fun drawHud(canvas: Canvas, w: Float, h: Float) {
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = w * 0.065f
        canvas.drawText("AR Roadster", w * 0.06f, h * 0.08f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Score: $score", w * 0.94f, h * 0.08f, paint)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = w * 0.042f
        canvas.drawText("Tap left or right to steer", w / 2f, h * 0.13f, paint)

        if (gameOver) {
            paint.color = 0xB8000000.toInt()
            canvas.drawRect(0f, 0f, w, h, paint)
            paint.color = Color.WHITE
            paint.textSize = w * 0.11f
            canvas.drawText("CRASH!", w / 2f, h * 0.46f, paint)
            paint.textSize = w * 0.055f
            canvas.drawText("Score: $score", w / 2f, h * 0.53f, paint)
            canvas.drawText("Tap to play again", w / 2f, h * 0.60f, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (gameOver) {
                obstacles.clear()
                playerLane = 0
                score = 0
                spawnTimer = 0f
                gameOver = false
            } else if (event.x < width / 2f) {
                playerLane = (playerLane - 1).coerceAtLeast(-1)
            } else {
                playerLane = (playerLane + 1).coerceAtMost(1)
            }
            invalidate()
            return true
        }
        return true
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float =
        start + (end - start) * fraction
}
