import Foundation
import SwiftData

@Model
final class PeriodRecord {
    var id: UUID
    var startDate: Date
    var endDate: Date?
    var cycleLength: Int
    var periodLength: Int
    var symptoms: String
    var mood: String
    var notes: String

    init(
        id: UUID = UUID(),
        startDate: Date,
        endDate: Date? = nil,
        cycleLength: Int = 28,
        periodLength: Int = 5,
        symptoms: String = "",
        mood: String = "",
        notes: String = ""
    ) {
        self.id = id
        self.startDate = startDate
        self.endDate = endDate
        self.cycleLength = cycleLength
        self.periodLength = periodLength
        self.symptoms = symptoms
        self.mood = mood
        self.notes = notes
    }
}
