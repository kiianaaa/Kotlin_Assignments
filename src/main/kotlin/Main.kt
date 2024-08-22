fun main() {
    val student1 = Student("Alice", 20)
    val student2 = Student("Bob", 22)

    student1.addCourse(CourseRecord("Math", 2023, 3, 85.0))
    student1.addCourse(CourseRecord("Physics", 2023, 4, 90.0))
    student1.addCourse(CourseRecord("Chemistry", 2024, 3, 75.0))

    student2.addCourse(CourseRecord("Math", 2023, 3, 78.0))
    student2.addCourse(CourseRecord("Physics", 2023, 4, 82.0))
    student2.addCourse(CourseRecord("Chemistry", 2024, 3, 88.0))

    val major = Major("Science")
    major.addStudent(student1)
    major.addStudent(student2)

    println(major.stats())
    println(major.stats("Math"))
}
