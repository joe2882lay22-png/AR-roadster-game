package com.example.arroadster

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Color
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode

class MainActivity : AppCompatActivity() {

    private lateinit var arFragment: ArFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        arFragment = supportFragmentManager
            .findFragmentById(R.id.arFragmentContainer) as ArFragment

        arFragment.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, motionEvent ->
            placeObject(hitResult)
        }
    }

    private fun placeObject(hitResult: HitResult) {
        val anchor = hitResult.createAnchor()
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(arFragment.arSceneView.scene)

        val cube = ShapeFactory.makeCube(
            Vector3(0.12f, 0.12f, 0.12f),
            Vector3(0.0f, 0.0f, 0.0f)
        )

        val material = cube.material
        material.setFloat3("color", Color(1f, 0.3f, 0.3f))

        val node = TransformableNode(arFragment.transformationSystem)
        node.renderable = cube
        node.setParent(anchorNode)
        node.select()
    }
}
