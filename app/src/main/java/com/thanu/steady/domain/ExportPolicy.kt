package com.thanu.steady.domain

import java.time.LocalDate

class ExportPolicy {
    fun generateMarkdown(
        date: LocalDate, 
        plan: DailyPlan?, 
        review: WeeklyReview?, 
        includeSensitive: Boolean = false
    ): String {
        val sb = StringBuilder()
        sb.append("# Steady Export: \$date\n\n")
        
        if (plan != null) {
            sb.append("## Daily Plan\n")
            sb.append("- **Mode**: \${plan.mode.name}\n")
            sb.append("- **Study**: \${plan.studyTask}\n")
            sb.append("- **Build**: \${plan.buildTask}\n")
            sb.append("- **Next Action**: \${plan.nextAction}\n")
            
            if (includeSensitive) {
                sb.append("- **Health**: \${plan.healthTask}\n")
                if (!plan.studyEvidence.isNullOrBlank()) {
                    sb.append("\n**Evidence**: \${plan.studyEvidence}\n")
                }
            }
            sb.append("\n")
        }
        
        if (review != null) {
            sb.append("## Weekly Review\n")
            sb.append("- **Helped**: \${review.helped}\n")
            sb.append("- **Too Demanding**: \${review.tooDemanding}\n")
            sb.append("- **Adjustment**: \${review.adjustment}\n")
            
            if (includeSensitive) {
                sb.append("### Indicators\n")
                sb.append("- Sleep: \${review.indicatorSleep}\n")
                sb.append("- Learning: \${review.indicatorLearning}\n")
                sb.append("- Building: \${review.indicatorBuilding}\n")
                sb.append("- Health: \${review.indicatorHealth}\n")
                sb.append("- Connection: \${review.indicatorConnection}\n")
            }
        }
        
        if (plan == null && review == null) {
            sb.append("_No records found for this date._\n")
        }
        
        return sb.toString()
    }
}
