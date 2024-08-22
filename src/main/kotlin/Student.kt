class Student (name: String, age: Int) : Human(name, age) {

    val courses = mutableListOf<CourseRecord>()

    fun addCourse(course: CourseRecord) {
        courses.add(course)
    }

    fun weightedAverage(): Double {
        val totalCredits = courses.sumOf { it.credits }
        val weightedSum = courses.sumOf { it.grade * it.credits }
        return if (totalCredits > 0) weightedSum / totalCredits else 0.0
    }

    fun weightedAvarage(year: Int): Double {
        val coursesInYear = courses.filter { it.yearCompleted == year }
        val totalCredits = coursesInYear.sumOf { it.credits }
        val weightedSum = coursesInYear.sumOf { it.grade * it.credits }
        return if (totalCredits > 0) weightedSum / totalCredits else 0.0
    }

    fun minMaxGrade(): Pair<Double, Double> {
        if (courses.isEmpty()) return Pair(0.0, 0.0)
        val grade = courses.map { it.grade }
        return Pair(grade.minOrNull() ?: 0.0, grade.maxOrNull() ?: 0.0)
    }


}