package com.workoutplanner.data.seed

import com.workoutplanner.data.db.Level
import com.workoutplanner.data.db.TargetType

/** A built-in exercise with its how-to steps. */
data class SeedExercise(val name: String, val muscleGroup: String, val steps: List<String>)

/** One row of a default plan table. Use [reps] or [seconds], not both. */
data class SeedRow(
    val exercise: String,
    val sets: Int,
    val reps: Int? = null,
    val seconds: Int? = null,
    val weightKg: Double? = null,
    val rest: Int = 90,
) {
    val targetType get() = if (seconds != null) TargetType.TIME else TargetType.REPS
    val target get() = seconds ?: reps ?: error("$exercise needs reps or seconds")
}

data class SeedPlan(val name: String, val description: String, val level: Level, val rows: List<SeedRow>)

/**
 * The plans and exercises that ship with the app, written in the same shape as the
 * in-app plan table. Inserted once by [DatabaseSeeder] on first launch.
 */
object DefaultPlans {

    val exercises = listOf(
        SeedExercise("Bodyweight Squat", "Legs", listOf(
            "Stand with feet shoulder-width apart, toes slightly out.",
            "Push hips back and bend knees as if sitting in a chair.",
            "Go down until thighs are parallel to the floor, chest up.",
            "Drive through your heels to stand back up.",
        )),
        SeedExercise("Push-up", "Chest", listOf(
            "Place hands under your shoulders, body in a straight line (knees down is fine).",
            "Brace your core and squeeze your glutes.",
            "Lower your chest to about a fist's height from the floor.",
            "Push back up and breathe out.",
        )),
        SeedExercise("Glute Bridge", "Glutes", listOf(
            "Lie on your back, knees bent, feet flat and hip-width apart.",
            "Press through your heels and lift your hips.",
            "Squeeze your glutes at the top for one second.",
            "Lower slowly back down.",
        )),
        SeedExercise("Plank", "Core", listOf(
            "Forearms on the floor, elbows under shoulders.",
            "Body in a straight line from head to heels.",
            "Brace your stomach as if about to be poked.",
            "Hold and breathe steadily - don't let your hips sag.",
        )),
        SeedExercise("Lunge", "Legs", listOf(
            "Stand tall, then take a big step forward.",
            "Lower until both knees are bent at about 90 degrees.",
            "Keep your front knee over your ankle.",
            "Push back to the start and switch legs.",
        )),
        SeedExercise("Bench Press", "Chest", listOf(
            "Lie on the bench with eyes under the bar, feet flat.",
            "Grip slightly wider than shoulders and unrack the bar.",
            "Lower it to your mid-chest with elbows about 45 degrees out.",
            "Press up until arms are straight. Use a spotter for heavy sets.",
        )),
        SeedExercise("Overhead Press", "Shoulders", listOf(
            "Stand with the bar at your collarbone, hands just outside shoulders.",
            "Brace your core and squeeze your glutes.",
            "Press the bar straight up, moving your head back out of the way.",
            "Lock out overhead, then lower under control.",
        )),
        SeedExercise("Incline Dumbbell Press", "Chest", listOf(
            "Set the bench to about 30 degrees.",
            "Hold dumbbells at chest level, palms forward.",
            "Press up and slightly together over your upper chest.",
            "Lower slowly until elbows are just below the bench.",
        )),
        SeedExercise("Tricep Pushdown", "Arms", listOf(
            "Face the cable with a bar or rope at chest height.",
            "Pin your elbows to your sides.",
            "Push down until your arms are straight.",
            "Return slowly without letting your elbows move forward.",
        )),
        SeedExercise("Lat Pulldown", "Back", listOf(
            "Sit with thighs under the pads, grip wider than shoulders.",
            "Lean back slightly and pull your shoulders down first.",
            "Pull the bar to your upper chest, elbows down and back.",
            "Let the bar rise slowly until your arms are straight.",
        )),
        SeedExercise("Seated Cable Row", "Back", listOf(
            "Sit tall with knees slightly bent, holding the handle.",
            "Pull the handle to your stomach, squeezing shoulder blades together.",
            "Keep your back straight - don't rock.",
            "Extend your arms slowly back to the start.",
        )),
        SeedExercise("Dumbbell Curl", "Arms", listOf(
            "Stand holding dumbbells at your sides, palms forward.",
            "Keep elbows close to your body.",
            "Curl the weights up to your shoulders.",
            "Lower slowly over about 2 seconds.",
        )),
        SeedExercise("Hammer Curl", "Arms", listOf(
            "Hold dumbbells at your sides, palms facing each other.",
            "Keep elbows pinned to your sides.",
            "Curl up while keeping the palms-in grip.",
            "Lower under control.",
        )),
        SeedExercise("Face Pull", "Shoulders", listOf(
            "Set a rope on the cable at upper-chest height.",
            "Pull the rope toward your face, hands splitting apart.",
            "Finish with elbows high and hands beside your ears.",
            "Return slowly.",
        )),
        SeedExercise("Barbell Squat", "Legs", listOf(
            "Bar on your upper back, feet shoulder-width apart.",
            "Brace your core, then sit down and back.",
            "Go to at least parallel, knees tracking over toes.",
            "Drive up through your whole foot. Use safety bars.",
        )),
        SeedExercise("Romanian Deadlift", "Hamstrings", listOf(
            "Hold the bar at hip height, knees slightly bent.",
            "Push your hips back, sliding the bar down your thighs.",
            "Stop when you feel a stretch in your hamstrings, back flat.",
            "Squeeze your glutes to stand back up.",
        )),
        SeedExercise("Leg Press", "Legs", listOf(
            "Sit with your back flat, feet shoulder-width on the platform.",
            "Release the safety handles.",
            "Lower until knees are at about 90 degrees.",
            "Press back up without locking your knees.",
        )),
        SeedExercise("Calf Raise", "Calves", listOf(
            "Stand with the balls of your feet on a step.",
            "Rise up onto your toes as high as you can.",
            "Pause for one second at the top.",
            "Lower your heels below the step for a stretch.",
        )),
        SeedExercise("Crunch", "Core", listOf(
            "Lie on your back, knees bent, hands lightly at your temples.",
            "Curl your shoulders off the floor using your stomach.",
            "Don't pull on your neck.",
            "Lower slowly.",
        )),
        SeedExercise("Leg Raise", "Core", listOf(
            "Lie flat with hands under your hips.",
            "Keep legs straight and lift them to vertical.",
            "Lower slowly without touching the floor.",
            "Keep your lower back pressed down.",
        )),
        SeedExercise("Russian Twist", "Core", listOf(
            "Sit with knees bent and lean back slightly.",
            "Lift your feet if you can.",
            "Rotate your shoulders to touch the floor on one side.",
            "Twist to the other side - each touch counts as one rep.",
        )),
        SeedExercise("Side Plank", "Core", listOf(
            "Lie on your side, elbow under your shoulder.",
            "Lift your hips so your body is a straight line.",
            "Hold, then switch sides - the time is per side.",
            "Don't let your hips drop.",
        )),
        SeedExercise("Mountain Climber", "Full body", listOf(
            "Start in a high plank, hands under shoulders.",
            "Drive one knee toward your chest.",
            "Quickly switch legs, like running in place.",
            "Keep your hips level.",
        )),

        // Dumbbell-only, no-bench exercises
        SeedExercise("Dumbbell Floor Press", "Chest", listOf(
            "Lie on the floor, knees bent, a dumbbell in each hand above your chest.",
            "Lower the dumbbells until your upper arms touch the floor, elbows about 45 degrees out.",
            "Pause for a moment with your arms on the floor.",
            "Press back up until your arms are straight.",
        )),
        SeedExercise("Dumbbell Floor Fly", "Chest", listOf(
            "Lie on the floor, dumbbells above your chest, palms facing each other.",
            "With a slight bend in your elbows, open your arms out wide.",
            "Stop when your upper arms touch the floor.",
            "Squeeze your chest to bring the dumbbells back together.",
        )),
        SeedExercise("Dumbbell Pullover", "Chest", listOf(
            "Lie on the floor, knees bent, holding one dumbbell with both hands above your chest.",
            "Keep a slight bend in your elbows.",
            "Lower the dumbbell back over your head until it nearly touches the floor.",
            "Pull it back over your chest using your chest and lats.",
        )),
        SeedExercise("Decline Push-up", "Chest", listOf(
            "Put your feet on a chair or step, hands on the floor under your shoulders.",
            "Keep your body in a straight line from head to heels.",
            "Lower your chest toward the floor.",
            "Push back up. Higher feet make it harder.",
        )),
        SeedExercise("Incline Push-up", "Chest", listOf(
            "Put your hands on a chair, bed or wall, slightly wider than your shoulders.",
            "Walk your feet back so your body is a straight line.",
            "Lower your chest to the edge.",
            "Push back up. A lower surface makes it harder.",
        )),
        SeedExercise("Standing Dumbbell Shoulder Press", "Shoulders", listOf(
            "Stand with dumbbells at shoulder height, palms forward.",
            "Brace your core and squeeze your glutes.",
            "Press the dumbbells overhead until your arms are straight.",
            "Lower slowly back to your shoulders. Don't arch your back.",
        )),
        SeedExercise("Arnold Press", "Shoulders", listOf(
            "Hold dumbbells in front of your shoulders, palms facing you.",
            "As you press up, rotate your palms to face forward.",
            "Finish with arms straight overhead.",
            "Reverse the rotation on the way down.",
        )),
        SeedExercise("Lateral Raise", "Shoulders", listOf(
            "Stand holding light dumbbells at your sides.",
            "With a slight bend in your elbows, raise your arms out to the sides.",
            "Stop at shoulder height, leading with your elbows.",
            "Lower slowly - don't swing.",
        )),
        SeedExercise("Bent-over Reverse Fly", "Shoulders", listOf(
            "Hinge forward at the hips until your chest is almost parallel to the floor.",
            "Let the dumbbells hang under your chest, palms facing each other.",
            "Raise your arms out to the sides, squeezing your shoulder blades.",
            "Lower slowly. Keep your back flat throughout.",
        )),
        SeedExercise("Front Raise", "Shoulders", listOf(
            "Stand holding dumbbells in front of your thighs.",
            "Raise one or both arms straight in front of you.",
            "Stop at shoulder height.",
            "Lower slowly without swinging your body.",
        )),
        SeedExercise("Dumbbell Shrug", "Traps", listOf(
            "Stand holding heavy dumbbells at your sides.",
            "Lift your shoulders straight up toward your ears.",
            "Hold the squeeze for one second.",
            "Lower slowly. Don't roll your shoulders.",
        )),
        SeedExercise("Overhead Triceps Extension", "Arms", listOf(
            "Stand holding one dumbbell with both hands above your head.",
            "Keep your elbows pointing forward, close to your head.",
            "Lower the dumbbell behind your head by bending your elbows.",
            "Straighten your arms to lift it back up.",
        )),
        SeedExercise("Diamond Push-up", "Arms", listOf(
            "Put your hands together under your chest, thumbs and index fingers making a diamond.",
            "Body in a straight line (knees down is fine).",
            "Lower your chest to your hands, elbows close to your sides.",
            "Push back up using your triceps.",
        )),
        SeedExercise("Concentration Curl (each arm)", "Arms", listOf(
            "Sit on a chair, elbow braced against the inside of your thigh.",
            "Let the dumbbell hang with your arm straight.",
            "Curl it up toward your shoulder, squeezing at the top.",
            "Lower slowly. Do all reps, then switch arms.",
        )),
        SeedExercise("Triceps Kickback (each arm)", "Arms", listOf(
            "Hinge forward, one hand on a chair or your knee for support.",
            "Hold the dumbbell with your upper arm against your side, elbow bent.",
            "Straighten your arm back until it's in line with your body.",
            "Bend back slowly. Do all reps, then switch arms.",
        )),
        SeedExercise("Bicycle Crunch", "Core", listOf(
            "Lie on your back, hands lightly at your temples, legs raised.",
            "Bring one elbow toward the opposite knee while straightening the other leg.",
            "Switch sides in a slow pedalling motion.",
            "Each side counts as one rep. Don't pull on your neck.",
        )),
        SeedExercise("Reverse Crunch", "Core", listOf(
            "Lie on your back, knees bent at 90 degrees, hands by your sides.",
            "Use your lower abs to curl your hips off the floor toward your chest.",
            "Pause at the top.",
            "Lower slowly without letting your feet drop.",
        )),
        SeedExercise("Dead Bug (each side)", "Core", listOf(
            "Lie on your back, arms up to the ceiling, knees bent at 90 degrees above your hips.",
            "Press your lower back into the floor.",
            "Slowly lower the opposite arm and leg toward the floor.",
            "Return and switch sides. Your back must stay flat.",
        )),
        SeedExercise("Hollow Body Hold", "Core", listOf(
            "Lie on your back, arms overhead, legs straight.",
            "Press your lower back into the floor.",
            "Lift your shoulders and legs a few centimetres off the floor.",
            "Hold. Bend your knees to make it easier.",
        )),

        // Warm-up moves: light cardio, joint circles and dynamic (moving) stretches
        SeedExercise("Jumping Jack", "Full body", listOf(
            "Stand tall, feet together, arms at your sides.",
            "Jump your feet out wide while swinging your arms overhead.",
            "Jump back to the start.",
            "Keep a light, steady rhythm. Step side to side instead for low impact.",
        )),
        SeedExercise("Arm Circles", "Shoulders", listOf(
            "Stand with your arms straight out to the sides at shoulder height.",
            "Make small forward circles, slowly getting bigger.",
            "Halfway through, switch to backward circles.",
            "Keep your shoulders down, away from your ears.",
        )),
        SeedExercise("Hip Circles", "Hips", listOf(
            "Stand with feet hip-width apart, hands on your hips.",
            "Draw big, slow circles with your hips.",
            "Halfway through, switch direction.",
            "Keep your upper body tall and your knees soft.",
        )),
        SeedExercise("Leg Swings (each leg)", "Hips", listOf(
            "Stand side-on to a wall and hold it for balance.",
            "Swing your outside leg forward and back, keeping it relaxed.",
            "Let each swing get a little higher without forcing it.",
            "Do all reps, then turn around and switch legs.",
        )),
        SeedExercise("Cat-Cow", "Back", listOf(
            "Start on hands and knees, hands under shoulders, knees under hips.",
            "Breathe in: drop your belly and lift your chest and tailbone (cow).",
            "Breathe out: round your back up toward the ceiling, chin to chest (cat).",
            "Move slowly with your breath. One cat plus one cow is one rep.",
        )),
        SeedExercise("World's Greatest Stretch (each side)", "Full body", listOf(
            "Step into a long lunge with your right foot forward.",
            "Put your left hand on the floor and drop your right elbow toward your right foot.",
            "Rotate your chest open and reach your right arm to the ceiling.",
            "Return, step back, and repeat on the other side. Each side counts as one rep.",
        )),
        SeedExercise("Inchworm", "Full body", listOf(
            "Stand tall, then bend forward and put your hands on the floor (bend knees if needed).",
            "Walk your hands forward until you're in a high plank.",
            "Hold for a second with your body in a straight line.",
            "Walk your feet up to your hands and stand back up.",
        )),
        SeedExercise("High Knees", "Full body", listOf(
            "Stand tall with your arms bent at your sides.",
            "Run in place, driving each knee up to hip height.",
            "Pump your arms and land softly on the balls of your feet.",
            "Marching in place is fine if you need lower impact.",
        )),
    )

    val plans = listOf(
        SeedPlan(
            name = "Beginner Full Body",
            description = "No equipment. A gentle start for every major muscle.",
            level = Level.BEGINNER,
            rows = listOf(
                SeedRow("Bodyweight Squat", sets = 3, reps = 12, rest = 60),
                SeedRow("Push-up", sets = 3, reps = 10, rest = 60),
                SeedRow("Glute Bridge", sets = 3, reps = 15, rest = 45),
                SeedRow("Plank", sets = 3, seconds = 30, rest = 45),
            ),
        ),
        SeedPlan(
            name = "Push Day",
            description = "Gym. Chest, shoulders and triceps.",
            level = Level.INTERMEDIATE,
            rows = listOf(
                SeedRow("Bench Press", sets = 4, reps = 8, weightKg = 40.0, rest = 120),
                SeedRow("Overhead Press", sets = 3, reps = 10, weightKg = 25.0, rest = 90),
                SeedRow("Incline Dumbbell Press", sets = 3, reps = 10, weightKg = 14.0, rest = 90),
                SeedRow("Tricep Pushdown", sets = 3, reps = 12, weightKg = 20.0, rest = 60),
            ),
        ),
        SeedPlan(
            name = "Pull Day",
            description = "Gym. Back, rear shoulders and biceps.",
            level = Level.INTERMEDIATE,
            rows = listOf(
                SeedRow("Lat Pulldown", sets = 4, reps = 10, weightKg = 40.0, rest = 90),
                SeedRow("Seated Cable Row", sets = 3, reps = 10, weightKg = 35.0, rest = 90),
                SeedRow("Dumbbell Curl", sets = 3, reps = 12, weightKg = 10.0, rest = 60),
                SeedRow("Face Pull", sets = 3, reps = 15, weightKg = 15.0, rest = 60),
            ),
        ),
        SeedPlan(
            name = "Leg Day",
            description = "Gym. Quads, hamstrings, glutes and calves.",
            level = Level.INTERMEDIATE,
            rows = listOf(
                SeedRow("Barbell Squat", sets = 4, reps = 8, weightKg = 50.0, rest = 120),
                SeedRow("Romanian Deadlift", sets = 3, reps = 10, weightKg = 40.0, rest = 120),
                SeedRow("Leg Press", sets = 3, reps = 12, weightKg = 80.0, rest = 90),
                SeedRow("Calf Raise", sets = 3, reps = 15, rest = 45),
            ),
        ),
        SeedPlan(
            name = "20-min Core",
            description = "No equipment. Short, focused core session.",
            level = Level.BEGINNER,
            rows = listOf(
                SeedRow("Crunch", sets = 3, reps = 15, rest = 30),
                SeedRow("Leg Raise", sets = 3, reps = 12, rest = 30),
                SeedRow("Russian Twist", sets = 3, reps = 20, rest = 30),
                SeedRow("Side Plank", sets = 2, seconds = 30, rest = 30),
            ),
        ),
        SeedPlan(
            name = "Dumbbell Chest",
            description = "Dumbbells only, no bench. About 30 min.",
            level = Level.BEGINNER,
            rows = listOf(
                SeedRow("Dumbbell Floor Press", sets = 4, reps = 10, weightKg = 10.0, rest = 90),
                SeedRow("Dumbbell Floor Fly", sets = 3, reps = 12, weightKg = 6.0, rest = 60),
                SeedRow("Dumbbell Pullover", sets = 3, reps = 12, weightKg = 8.0, rest = 60),
                SeedRow("Decline Push-up", sets = 3, reps = 10, rest = 45),
                SeedRow("Incline Push-up", sets = 3, reps = 15, rest = 45),
            ),
        ),
        SeedPlan(
            name = "Dumbbell Shoulders",
            description = "Dumbbells only, no bench. About 30 min.",
            level = Level.BEGINNER,
            rows = listOf(
                SeedRow("Standing Dumbbell Shoulder Press", sets = 4, reps = 10, weightKg = 8.0, rest = 90),
                SeedRow("Arnold Press", sets = 3, reps = 10, weightKg = 6.0, rest = 60),
                SeedRow("Lateral Raise", sets = 3, reps = 15, weightKg = 4.0, rest = 45),
                SeedRow("Bent-over Reverse Fly", sets = 3, reps = 15, weightKg = 4.0, rest = 45),
                SeedRow("Front Raise", sets = 3, reps = 12, weightKg = 4.0, rest = 45),
                SeedRow("Dumbbell Shrug", sets = 3, reps = 15, weightKg = 12.0, rest = 45),
            ),
        ),
        SeedPlan(
            name = "Dumbbell Arms",
            description = "Dumbbells only, no bench. Biceps and triceps alternate. About 30 min.",
            level = Level.BEGINNER,
            rows = listOf(
                SeedRow("Dumbbell Curl", sets = 3, reps = 12, weightKg = 8.0, rest = 60),
                SeedRow("Overhead Triceps Extension", sets = 3, reps = 12, weightKg = 8.0, rest = 60),
                SeedRow("Hammer Curl", sets = 3, reps = 12, weightKg = 8.0, rest = 60),
                SeedRow("Diamond Push-up", sets = 3, reps = 10, rest = 60),
                SeedRow("Concentration Curl (each arm)", sets = 3, reps = 10, weightKg = 6.0, rest = 45),
                SeedRow("Triceps Kickback (each arm)", sets = 3, reps = 12, weightKg = 4.0, rest = 45),
            ),
        ),
        SeedPlan(
            name = "Core Strength",
            description = "No equipment. Harder and longer than 20-min Core. About 30 min.",
            level = Level.INTERMEDIATE,
            rows = listOf(
                SeedRow("Bicycle Crunch", sets = 3, reps = 20, rest = 45),
                SeedRow("Reverse Crunch", sets = 3, reps = 15, rest = 45),
                SeedRow("Dead Bug (each side)", sets = 3, reps = 10, rest = 45),
                SeedRow("Hollow Body Hold", sets = 3, seconds = 30, rest = 45),
                SeedRow("Mountain Climber", sets = 3, seconds = 30, rest = 45),
                SeedRow("Plank", sets = 3, seconds = 45, rest = 45),
                SeedRow("Side Plank", sets = 2, seconds = 30, rest = 30),
            ),
        ),

        // Warm-ups. Rests are short on purpose: they are only the time to change moves.
        SeedPlan(
            name = "5-min Warm-up",
            description = "No equipment. Quick warm-up before any workout.",
            level = Level.BEGINNER,
            rows = listOf(
                SeedRow("Jumping Jack", sets = 1, seconds = 45, rest = 10),
                SeedRow("Arm Circles", sets = 1, seconds = 30, rest = 10),
                SeedRow("Hip Circles", sets = 1, seconds = 30, rest = 10),
                SeedRow("Leg Swings (each leg)", sets = 1, reps = 10, rest = 10),
                SeedRow("Bodyweight Squat", sets = 1, reps = 10, rest = 10),
                SeedRow("Inchworm", sets = 1, reps = 5, rest = 10),
            ),
        ),
        SeedPlan(
            name = "10-min Warm-up",
            description = "No equipment. Full warm-up before heavy lifting or leg day.",
            level = Level.BEGINNER,
            rows = listOf(
                SeedRow("Jumping Jack", sets = 2, seconds = 45, rest = 15),
                SeedRow("Arm Circles", sets = 1, seconds = 40, rest = 10),
                SeedRow("Cat-Cow", sets = 1, reps = 8, rest = 10),
                SeedRow("Hip Circles", sets = 1, seconds = 30, rest = 10),
                SeedRow("Leg Swings (each leg)", sets = 1, reps = 10, rest = 10),
                SeedRow("World's Greatest Stretch (each side)", sets = 1, reps = 5, rest = 10),
                SeedRow("Inchworm", sets = 1, reps = 6, rest = 15),
                SeedRow("Bodyweight Squat", sets = 2, reps = 10, rest = 15),
                SeedRow("High Knees", sets = 2, seconds = 30, rest = 20),
            ),
        ),
    )
}
