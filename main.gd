extends Node3D

const ROAD_WIDTH := 10.0
const ROAD_LENGTH := 180.0
const CAR_SPEED := 24.0
const STEER_SPEED := 7.0

var car: Node3D
var lane_marks: Array[MeshInstance3D] = []
var roadside: Array[Node3D] = []
var touch_positions: Dictionary = {}
var touch_steer := 0.0
var driving := false
var distance := 0.0
var speed_text: Label


func _ready() -> void:
	_build_world()
	_build_road()
	_build_car()
	_build_ui()


func _build_world() -> void:
	var world := WorldEnvironment.new()
	var environment := Environment.new()
	environment.background_mode = Environment.BG_SKY

	var sky := Sky.new()
	var sky_material := ProceduralSkyMaterial.new()
	sky_material.sky_top_color = Color(0.10, 0.25, 0.48)
	sky_material.sky_horizon_color = Color(0.72, 0.82, 0.92)
	sky_material.ground_bottom_color = Color(0.12, 0.24, 0.12)
	sky_material.ground_horizon_color = Color(0.48, 0.58, 0.42)
	sky.sky_material = sky_material

	environment.sky = sky
	environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
	environment.ambient_light_color = Color(0.78, 0.82, 0.90)
	environment.ambient_light_energy = 0.8
	world.environment = environment
	add_child(world)

	var sun := DirectionalLight3D.new()
	sun.rotation_degrees = Vector3(-48, -28, 0)
	sun.light_energy = 1.25
	add_child(sun)


func _build_road() -> void:
	var grass := MeshInstance3D.new()
	var grass_mesh := PlaneMesh.new()
	grass_mesh.size = Vector2(220, ROAD_LENGTH + 40)
	grass.mesh = grass_mesh
	grass.position = Vector3(0, -0.19, -70)
	grass.material_override = _mat(Color(0.16, 0.36, 0.18))
	add_child(grass)

	var road := MeshInstance3D.new()
	var road_mesh := BoxMesh.new()
	road_mesh.size = Vector3(ROAD_WIDTH, 0.18, ROAD_LENGTH)
	road.mesh = road_mesh
	road.position = Vector3(0, -0.08, -70)
	road.material_override = _mat(Color(0.12, 0.14, 0.17))
	add_child(road)

	for z in range(-155, 12, 12):
		var dash := _box(Vector3(0.16, 0.035, 5.0), Color(1.0, 0.86, 0.48))
		dash.position = Vector3(0, 0.025, z)
		add_child(dash)
		lane_marks.append(dash)

	for side in [-1, 1]:
		var edge := _box(
			Vector3(0.12, 0.04, ROAD_LENGTH),
			Color(0.88, 0.88, 0.82)
		)
		edge.position = Vector3(side * (ROAD_WIDTH * 0.5 - 0.35), 0.025, -70)
		add_child(edge)

	for z in range(-150, 25, 18):
		for side in [-1, 1]:
			var tree := Node3D.new()
			tree.position = Vector3(
				side * randf_range(8.5, 13.0),
				0,
				z + randf_range(-3.0, 3.0)
			)

			var trunk := _box(Vector3(0.38, 1.8, 0.38), Color(0.30, 0.17, 0.08))
			trunk.position.y = 0.8
			tree.add_child(trunk)

			var crown := MeshInstance3D.new()
			var crown_mesh := SphereMesh.new()
			crown_mesh.radius = 1.55
			crown_mesh.height = 3.1
			crown.mesh = crown_mesh
			crown.position.y = 2.45
			crown.material_override = _mat(
				Color(0.10, randf_range(0.30, 0.47), 0.15)
			)
			tree.add_child(crown)

			add_child(tree)
			roadside.append(tree)


func _build_car() -> void:
	car = Node3D.new()
	car.position = Vector3(0, 0, 1.5)
	add_child(car)

	_add_car_box(
		Vector3(1.8, 0.42, 3.25),
		Vector3(0, 0.55, 0),
		Color(0.82, 0.045, 0.025)
	)
	_add_car_box(
		Vector3(1.36, 0.62, 1.48),
		Vector3(0, 1.0, -0.25),
		Color(0.10, 0.22, 0.29)
	)
	_add_car_box(
		Vector3(1.82, 0.16, 0.76),
		Vector3(0, 0.70, 0.88),
		Color(0.93, 0.09, 0.025)
	)

	for side in [-1, 1]:
		for z in [-1.0, 1.0]:
			var wheel := MeshInstance3D.new()
			var wheel_mesh := CylinderMesh.new()
			wheel_mesh.top_radius = 0.37
			wheel_mesh.bottom_radius = 0.37
			wheel_mesh.height = 0.24
			wheel.mesh = wheel_mesh
			wheel.rotation_degrees.z = 90
			wheel.position = Vector3(side * 0.91, 0.38, z)
			wheel.material_override = _mat(Color(0.035, 0.04, 0.045))
			car.add_child(wheel)

		for z in [-1.66, 1.66]:
			var lamp_color: Color
			if z > 0:
				lamp_color = Color(1.0, 0.86, 0.45)
			else:
				lamp_color = Color(0.9, 0.02, 0.01)

			var lamp := _box(Vector3(0.3, 0.12, 0.05), lamp_color)
			lamp.position = Vector3(side * 0.55, 0.62, z)
			car.add_child(lamp)

	var camera := Camera3D.new()
	camera.position = Vector3(0, 4.3, 8.5)
	camera.rotation_degrees = Vector3(-15, 0, 0)
	camera.current = true
	car.add_child(camera)


func _add_car_box(size: Vector3, pos: Vector3, color: Color) -> void:
	var part := _box(size, color)
	part.position = pos
	car.add_child(part)


func _box(size: Vector3, color: Color) -> MeshInstance3D:
	var item := MeshInstance3D.new()
	var box_mesh := BoxMesh.new()
	box_mesh.size = size
	item.mesh = box_mesh
	item.material_override = _mat(color)
	return item


func _mat(color: Color) -> StandardMaterial3D:
	var material := StandardMaterial3D.new()
	material.albedo_color = color
	material.roughness = 0.75
	return material


func _build_ui() -> void:
	var layer := CanvasLayer.new()
	add_child(layer)

	var panel := ColorRect.new()
	panel.color = Color(0.02, 0.04, 0.08, 0.68)
	panel.position = Vector2(16, 16)
	panel.size = Vector2(300, 88)
	layer.add_child(panel)

	var title := Label.new()
	title.text = "ROADSTER DRIVE"
	title.position = Vector2(30, 23)
	title.add_theme_font_size_override("font_size", 24)
	title.add_theme_color_override("font_color", Color(1.0, 0.82, 0.25))
	layer.add_child(title)

	speed_text = Label.new()
	speed_text.position = Vector2(30, 57)
	speed_text.add_theme_font_size_override("font_size", 18)
	layer.add_child(speed_text)

	var hint := Label.new()
	hint.text = "ထိပြီးဖိထား၍ မောင်းပါ · ဖိထားရင်း ဘယ်/ညာရွှေ့၍ ကွေ့ပါ"
	hint.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	hint.anchor_left = 0.04
	hint.anchor_right = 0.96
	hint.anchor_top = 0.88
	hint.anchor_bottom = 0.98
	hint.add_theme_font_size_override("font_size", 17)
	hint.add_theme_color_override("font_color", Color.WHITE)
	layer.add_child(hint)


func _input(event: InputEvent) -> void:
	if event is InputEventScreenTouch:
		if event.pressed:
			touch_positions[event.index] = event.position
		else:
			touch_positions.erase(event.index)
	elif event is InputEventScreenDrag:
		touch_positions[event.index] = event.position

	_update_touch_control()


func _update_touch_control() -> void:
	driving = not touch_positions.is_empty()
	touch_steer = 0.0

	if not driving:
		return

	var screen_center := get_viewport().get_visible_rect().size.x * 0.5
	var steering_total := 0.0

	for point in touch_positions.values():
		steering_total += clampf((point.x - screen_center) / screen_center, -1.0, 1.0)

	touch_steer = clampf(steering_total, -1.0, 1.0)


func _process(delta: float) -> void:
	var key_steer := Input.get_axis("ui_left", "ui_right")
	var target_steer := touch_steer if driving else key_steer
	var target_x := clampf(
		car.position.x + target_steer * STEER_SPEED * delta,
		-3.2,
		3.2
	)

	car.position.x = move_toward(car.position.x, target_x, STEER_SPEED * delta)
	car.rotation.z = -target_steer * 0.06

	var throttle := 1.0 if driving else 0.45
	var current_speed := CAR_SPEED * throttle
	distance += current_speed * delta

	for mark in lane_marks:
		mark.position.z += current_speed * delta
		if mark.position.z > 12.0:
			mark.position.z -= 168.0

	for tree in roadside:
		tree.position.z += current_speed * delta
		if tree.position.z > 18.0:
			tree.position.z -= 180.0

	speed_text.text = "အမြန်နှုန်း  %03d km/h    ခရီး  %04d m" % [
		int(current_speed * 7.0),
		int(distance)
	]
