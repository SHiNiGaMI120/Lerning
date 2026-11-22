package com.example

import com.example.data.curriculum.CurriculumCatalog
import com.example.data.curriculum.ExpandedCurriculum
import com.example.data.model.*
import org.junit.Assert.*
import org.junit.Test

class ExpandedCurriculumTest {
    @Test fun everyLanguageLevelAndUnitHasTwoAuthoredTargetsAndValidQuizzes() {
        val newTargets = mutableSetOf<String>()
        for (language in TargetLanguage.entries) for (level in CefrLevel.entries) {
            val units = CurriculumCatalog.unitsFor(language, level)
            assertEquals(12, units.size)
            val newIds = mutableSetOf<String>()
            for (unit in units) {
                val authored = ExpandedCurriculum.itemsFor(language, level, unit.number, unit.titleAr)
                assertEquals(2, authored.size)
                for (item in authored) {
                    assertTrue(newIds.add(item.id))
                    assertTrue("Duplicate new target: ${item.sentence}", newTargets.add(item.sentence))
                    assertEquals(language, item.language)
                    assertEquals(level, item.cefrLevel)
                    assertEquals(unit.number, item.unitNumber)
                    assertTrue(item.sentence.isNotBlank())
                    assertTrue(item.arabicTranslation.any { it in '\u0600'..'\u06FF' })
                    assertTrue(item.expressionNoteAr.isNotBlank())
                    assertTrue(item.grammarInContextAr.isNotBlank())
                    assertTrue(item.realWorldUsageAr.contains(unit.titleAr))
                    assertEquals(1, item.similarExamples.size)
                    assertTrue(item.similarExamples.single().first != item.sentence)
                    assertEquals(3, item.optionsAr.distinct().size)
                    assertTrue(item.correctOptionIndex in item.optionsAr.indices)
                    assertEquals(item.arabicTranslation, item.optionsAr[item.correctOptionIndex])
                    assertTrue(item.dialectVariantAlt.isBlank())
                }
            }
            assertEquals(24, newIds.size)
        }
        assertEquals(288, newTargets.size)
    }

    @Test fun all1440LessonsKeepStableIdsAndUnitMatchedContent() {
        for (language in TargetLanguage.entries) for (level in CefrLevel.entries) {
            val unique = mutableSetOf<String>()
            for (unit in 1..12) for (lesson in 1..10) {
                val items = CurriculumCatalog.sentencesFor(language, level, unit, lesson)
                assertEquals(4, items.size)
                assertEquals(4, items.map { it.sentence }.distinct().size)
                val authored = ExpandedCurriculum.itemsFor(language, level, unit, "test").map { it.sentence }.toSet()
                assertEquals(authored, items.take(2).map { it.sentence }.toSet())
                for ((index, item) in items.withIndex()) {
                    assertEquals("${language.code.lowercase()}_${level.code.lowercase()}_u${unit}_l${lesson}_s${index + 1}", item.id)
                    assertEquals(unit, item.unitNumber)
                    assertEquals(lesson, item.lessonNumber)
                    assertEquals(language, item.language)
                    assertEquals(level, item.cefrLevel)
                    assertTrue(item.optionsAr.distinct().size >= 3)
                    assertTrue(item.correctOptionIndex in item.optionsAr.indices)
                    unique.add(item.sentence)
                }
            }
            assertEquals(28, unique.size)
        }
    }

    @Test fun examsGrowTo14QuestionsWithoutDuplicateIdsOrInvalidAnswers() {
        for (language in TargetLanguage.entries) for (level in CefrLevel.entries) {
            val questions = CurriculumCatalog.examQuestionsFor(language, level)
            assertEquals(14, questions.size)
            assertEquals(14, questions.map { it.id }.toSet().size)
            assertEquals(7, questions.count { it.skill == SkillType.LISTENING })
            assertEquals(7, questions.count { it.skill == SkillType.READING })
            for (question in questions) {
                assertTrue(question.stimulusTarget.isNotBlank())
                assertTrue(question.correctIndex in question.options.indices)
                assertEquals(3, question.options.distinct().size)
            }
            assertEquals(setOf(0, 1, 2), questions.takeLast(12).map { it.correctIndex }.toSet())
        }
    }

    @Test fun advancedLevelsAndLanguagesHaveDifferentNewBanks() {
        fun bank(language: TargetLanguage, level: CefrLevel) = (1..12).flatMap {
            ExpandedCurriculum.itemsFor(language, level, it, "test")
        }.map { it.sentence }.toSet()
        for (language in TargetLanguage.entries) {
            assertTrue(bank(language, CefrLevel.B1).intersect(bank(language, CefrLevel.B2)).isEmpty())
            assertTrue(bank(language, CefrLevel.C1).intersect(bank(language, CefrLevel.C2)).isEmpty())
        }
        assertTrue(bank(TargetLanguage.EN, CefrLevel.A1).intersect(bank(TargetLanguage.ES, CefrLevel.A1)).isEmpty())
    }

    @Test fun invalidUnitOrLessonIsRejectedRatherThanSilentlyWrapped() {
        for ((unit, lesson) in listOf(0 to 1, 13 to 1, 1 to 0, 1 to 11)) {
            assertThrows(IllegalArgumentException::class.java) {
                CurriculumCatalog.sentencesFor(TargetLanguage.EN, CefrLevel.A1, unit, lesson)
            }
        }
    }
}
