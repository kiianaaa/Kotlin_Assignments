class Major(val name: String) {

    private val students = mutableListOf<Student>()

    fun addStudent(student: Student) {
        students.add(student)
    }

    fun stats(): Triple<Double, Double, Double> {
        if (students.isEmpty()) return Triple(0.0, 0.0, 0.0)
        val averages = students.map { it.weightedAverage() }
        val minAvg = averages.minOrNull() ?: 0.0
        val maxAvg = averages.maxOrNull() ?: 0.0
        val avgAvg = averages.average()
        return Triple(minAvg, maxAvg, avgAvg)
    }

    fun stats(courseName: String): Triple<Double, Double, Double> {
        val averages = students.mapNotNull {
            val course = it.courses.find { course -> course.name == courseName }
            course?.let { it.grade * it.credits }
        }

        if (averages.isEmpty()) return Triple(0.0, 0.0, 0.0)

        val minAvg = averages.minOrNull() ?: 0.0
        val maxAvg = averages.maxOrNull() ?: 0.0
        val avgAvg = averages.average()
        return Triple(minAvg, maxAvg, avgAvg)
    }
}
