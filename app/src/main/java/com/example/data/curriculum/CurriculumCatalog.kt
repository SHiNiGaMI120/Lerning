package com.example.data.curriculum

import com.example.data.model.CefrLevel
import com.example.data.model.CurriculumUnit
import com.example.data.model.ExamQuestion
import com.example.data.model.SentenceLessonItem
import com.example.data.model.SkillType
import com.example.data.model.TargetLanguage
import com.example.data.model.TutorScenario

object CurriculumCatalog {

    private val unitThemesByLevel: Map<CefrLevel, List<Pair<String, String>>> = mapOf(
        CefrLevel.A1 to listOf(
            "التعارف والتحية اليومية" to "حياة يومية",
            "العائلة والأصدقاء" to "علاقات",
            "في المقهى والمطعم" to "طعام",
            "التنقل والمواصلات" to "سفر",
            "التسوق والأسعار" to "تسوق",
            "الروتين الصباحي والوقت" to "روتين",
            "حجز الفندق والغرف" to "سفر",
            "السؤال عن الاتجاهات" to "مدينة",
            "الطقس والفصول" to "طبيعة",
            "الهوايات والرياضة" to "ترفيه",
            "الصحة والعيادة" to "صحة",
            "الخطط البسيطة لعطلة الأسبوع" to "تخطيط"
        ),
        CefrLevel.A2 to listOf(
            "الذكريات وتجارب الماضي" to "سرد",
            "في المطار وتأخير الرحلات" to "سفر",
            "وصف الوظيفة وبيئة العمل" to "عمل",
            "المقارنات واختيار الأفضل" to "تسوق",
            "الدراسة والجامعة" to "دراسة",
            "المكالمات الهاتفية والرسائل" to "تواصل",
            "حل المشكلات اليومية" to "مواقف",
            "الثقافة والعادات الاجتماعية" to "ثقافة",
            "التكنولوجيا والتطبيقات" to "تقنية",
            "التسوق الإلكتروني والإرجاع" to "تسوق",
            "تنظيم رحلة مع الأصدقاء" to "مغامرة",
            "نصائح العادات الصحية" to "صحة"
        ),
        CefrLevel.B1 to listOf(
            "إبداء الرأي والموافقة والاعتراض" to "نقاش",
            "مقابلة العمل الاحترافية" to "عمل",
            "سرد القصص والمفاجآت" to "قصص",
            "الإعلام والأخبار اليومية" to "إعلام",
            "البيئة والاستدامة" to "مجتمع",
            "العمل عن بُعد وإدارة الوقت" to "إنتاجية",
            "الشكاوى المهذبة وطلب التعويض" to "خدمات",
            "ريادة الأعمال والمشاريع الناشئة" to "أعمال",
            "الفنون والسينما والكتب" to "ثقافة",
            "التفاوض في المواقف الاجتماعية" to "تواصل",
            "التخطيط للمستقبل المهني" to "طموح",
            "فهم النكات والتعبيرات الدارجة" to "كوميديا"
        ),
        CefrLevel.B2 to listOf(
            "إدارة الاجتماعات والعروض التقديمية" to "أعمال",
            "التحليل النقدي للأخبار والمقالات" to "قراءة",
            "التعبيرات الاصطلاحية في العمل" to "تعبيرات",
            "الدبلوماسية والإقناع المهني" to "تفاوض",
            "القانون اليومي والعقود البسيطة" to "مهني",
            "الذكاء الاصطناعي ومستقبل الوظائف" to "تقنية",
            "الاقتصاد الشخصي والاستثمار" to "مالية",
            "النقاشات الأخلاقية والمجتمعية" to "فكر",
            "فهم اللهجات الإقليمية السريعة" to "استماع",
            "كتابة التقارير ورسائل التغطية" to "كتابة",
            "حل النزاعات داخل الفرق" to "قيادة",
            "تلخيص المحاضرات والمقابلات" to "أكاديمي"
        ),
        CefrLevel.C1 to listOf(
            "الخطاب الأكاديمي وبناء الحجج" to "أكاديمي",
            "المعاني الضمنية والسخرية اللطيفة" to "تداولية",
            "المفاوضات التجارية عالية المستوى" to "قيادة",
            "الأدب الحديث والأساليب السردية" to "أدب",
            "الخطابة العامة والتأثير" to "إلقاء",
            "السياسة العامة والتحليل الاقتصادي" to "تحليل",
            "التعبيرات الثقافية المتقدمة" to "ثقافة",
            "الكتابة التحليلية والمقالات الرصينة" to "كتابة",
            "فهم الحوارات المتداخلة السريعة" to "استماع",
            "التفكير النقدي وتفنيد المغالطات" to "منطق",
            "الإدارة التنفيذية وإدارة الأزمات" to "أعمال",
            "صياغة الفروق الدلالية الدقيقة" to "دقة"
        ),
        CefrLevel.C2 to listOf(
            "البلاغة والإيجاز بأسلوب المتحدث الأصلي" to "بلاغة",
            "تحليل الخطاب الإعلامي والسياسي" to "نقد",
            "الترجمة الإبداعية ونقل الروح الثقافية" to "ترجمة",
            "المناظرات الفلسفية والعلمية" to "مناظرة",
            "صياغة الأوراق البحثية والتنفيذية" to "بحث",
            "إتقان اللهجات والنبرة الإيحائية" to "نطق",
            "الكتابة الإبداعية والقصصية المتقنة" to "إبداع",
            "الإقناع الاستراتيجي والتفاوض الدولي" to "استراتيجية",
            "تفكيك التعبيرات التراثية والمعاصرة" to "تراث",
            "التعليق الفوري والارتجال المنظم" to "ارتجال",
            "المراجعة التحريرية وتنقيح النصوص" to "تحرير",
            "مشروع الإتقان الشامل لمستوى C2" to "إتقان"
        )
    )

    private val lessonFlowTemplatesAr = listOf(
        "حوار الموقف الأساسي",
        "التعبيرات الطبيعية للمتحدث الأصلي",
        "القواعد داخل السياق الحي",
        "توسيع الجمل والردود البديلة",
        "تحدي الاستماع باللهجتين",
        "تدريب النطق والتنغيم (Shadowing)",
        "القراءة السياقية واستنتاج المعنى",
        "الكتابة التطبيقية للموقف",
        "محاكاة حوارية سريعة",
        "مراجعة واختبار نهاية الوحدة"
    )

    fun unitsFor(language: TargetLanguage, level: CefrLevel): List<CurriculumUnit> {
        val themes = unitThemesByLevel[level].orEmpty()
        return themes.mapIndexed { idx, (title, tag) ->
            CurriculumUnit(
                number = idx + 1,
                cefrLevel = level,
                language = language,
                titleAr = title,
                themeTagAr = tag,
                lessonTitlesAr = lessonFlowTemplatesAr.mapIndexed { lIdx, name ->
                    "الدرس ${lIdx + 1}: $name"
                }
            )
        }
    }

    fun sentencesFor(
        language: TargetLanguage,
        level: CefrLevel,
        unitNumber: Int,
        lessonNumber: Int
    ): List<SentenceLessonItem> {
        require(unitNumber in 1..12) { "Unit must be between 1 and 12" }
        require(lessonNumber in 1..10) { "Lesson must be between 1 and 10" }
        val bank = if (language == TargetLanguage.EN) englishBank(level) else spanishBank(level)
        val theme = unitThemesByLevel.getValue(level)[unitNumber - 1].first
        val themed = ExpandedCurriculum.itemsFor(language, level, unitNumber, theme)
        // Keep the unit's two new targets, then revisit two earlier base examples.
        // Ten lesson activities intentionally revisit a unit's language for retrieval practice.
        val reviewOffset = (lessonNumber - 1) % bank.size
        val chosen = (if (lessonNumber % 2 == 0) themed.reversed() else themed) +
            listOf(bank[reviewOffset], bank[(reviewOffset + 1) % bank.size])
        return chosen.mapIndexed { index, item ->
            item.copy(
                id = "${language.code.lowercase()}_${level.code.lowercase()}_u${unitNumber}_l${lessonNumber}_s${index + 1}",
                unitNumber = unitNumber, lessonNumber = lessonNumber
            )
        }
    }

    private fun englishBank(level: CefrLevel): List<SentenceLessonItem> = when (level) {
        CefrLevel.A1 -> listOf(
            SentenceLessonItem(
                id = "en_a1_1", language = TargetLanguage.EN, cefrLevel = CefrLevel.A1, unitNumber = 1, lessonNumber = 1,
                sentence = "Hi, nice to meet you! Could I get a hot coffee to go?",
                dialectVariantAlt = "UK: Hi, lovely to meet you! Could I have a takeaway coffee, please?",
                arabicTranslation = "مرحبًا، سررت بلقائك! هل يمكنني الحصول على قهوة ساخنة سفري؟",
                expressionNoteAr = "تعبير 'to go' يُستخدم في الأمريكية للطلبات السفري، بينما 'takeaway' شائع في البريطانية.",
                grammarInContextAr = "استخدام 'Could I get...?' للطلب المهذب جدًا بدلًا من الأمر المباشر.",
                realWorldUsageAr = "تُقال يوميًا في المقاهي وعند التعارف السريع في بيئة ودودة.",
                similarExamples = listOf(
                    "Could I get a bottle of water, please?" to "هل يمكنني الحصول على زجاجة ماء من فضلك؟",
                    "Nice to meet you, I'm Rami from Cairo." to "سررت بلقائك، أنا رامي من القاهرة."
                ),
                comprehensionQuestionAr = "ماذا يعني تعبير (to go) في هذه الجملة؟",
                optionsAr = listOf("للشرب داخل المقهى", "طلب سفري خارج المقهى", "قهوة باردة بالثلج"),
                correctOptionIndex = 1
            ),
            SentenceLessonItem(
                id = "en_a1_2", language = TargetLanguage.EN, cefrLevel = CefrLevel.A1, unitNumber = 1, lessonNumber = 2,
                sentence = "Excuse me, how do I get to the central train station from here?",
                dialectVariantAlt = "UK: Excuse me, could you point me towards the main railway station?",
                arabicTranslation = "عفوًا، كيف أصل إلى محطة القطار المركزية من هنا؟",
                expressionNoteAr = "'How do I get to...' هي الصيغة الطبيعية الأكثر استخدامًا للسؤال عن الطريق.",
                grammarInContextAr = "نستخدم المضارع البسيط مع (do) للسؤال عن الإرشادات والطرق الثابتة.",
                realWorldUsageAr = "مناسبة عند سؤال المارة في الشارع أو موظف الاستقبال في الفندق.",
                similarExamples = listOf(
                    "How do I get to the nearest pharmacy?" to "كيف أصل إلى أقرب صيدلية؟",
                    "Is the bus stop far from here?" to "هل موقف الحافلة بعيد عن هنا؟"
                ),
                comprehensionQuestionAr = "ما الهدف الرئيسي من الجملة؟",
                optionsAr = listOf("حجز تذكرة قطار", "السؤال عن الاتجاهات لمحطة القطار", "شراء خريطة"),
                correctOptionIndex = 1
            ),
            SentenceLessonItem(
                id = "en_a1_3", language = TargetLanguage.EN, cefrLevel = CefrLevel.A1, unitNumber = 1, lessonNumber = 3,
                sentence = "I have a reservation under the name Tariq for three nights.",
                dialectVariantAlt = "UK: I've booked a room under the name Tariq for three nights.",
                arabicTranslation = "لدي حجز باسم طارق لمدة ثلاث ليالٍ.",
                expressionNoteAr = "عبارة 'under the name' تعني 'باسمِ كذا' في الحجوزات الفندقية والمطاعم.",
                grammarInContextAr = "حرف الجر 'for' يُستخدم قبل المدة الزمنية (three nights).",
                realWorldUsageAr = "الجملة الأساسية عند مكتب الاستقبال في أي فندق حول العالم.",
                similarExamples = listOf(
                    "Does the room include breakfast?" to "هل الغرفة تشمل الإفطار؟",
                    "What time is check-out tomorrow?" to "متى موعد المغادرة غدًا؟"
                ),
                comprehensionQuestionAr = "ما معنى (under the name) في سياق الفنادق؟",
                optionsAr = listOf("تحت الطاولة", "مسجل باسم", "رقم الغرفة"),
                correctOptionIndex = 1
            ),
            SentenceLessonItem(
                id = "en_a1_4", language = TargetLanguage.EN, cefrLevel = CefrLevel.A1, unitNumber = 1, lessonNumber = 4,
                sentence = "How much does this jacket cost, and do you have it in medium?",
                dialectVariantAlt = "UK: How much is this jacket, and have you got it in a medium?",
                arabicTranslation = "كم سعر هذه السترة، وهل لديكم منها مقاس متوسط؟",
                expressionNoteAr = "'in medium' تعني بالمقاس المتوسط دون الحاجة لقول size في كل مرة.",
                grammarInContextAr = "استخدام 'does' مع المفرد (this jacket) و'do' مع المخاطب (you).",
                realWorldUsageAr = "مفيدة جدًا في متاجر الملابس والأسواق اليومية.",
                similarExamples = listOf(
                    "Can I try this on in the fitting room?" to "هل يمكنني قياس هذا في غرفة القياس؟",
                    "Do you accept contactless cards?" to "هل تقبلون البطاقات اللاتلامسية؟"
                ),
                comprehensionQuestionAr = "ماذا يطلب المتحدث بالإضافة إلى معرفة السعر؟",
                optionsAr = listOf("تغيير اللون", "توفّر المقاس المتوسط", "خصم خاص"),
                correctOptionIndex = 1
            )
        )
        CefrLevel.A2 -> listOf(
            SentenceLessonItem(
                id = "en_a2_1", language = TargetLanguage.EN, cefrLevel = CefrLevel.A2, unitNumber = 1, lessonNumber = 1,
                sentence = "My flight was delayed by two hours, so I missed my connecting train.",
                dialectVariantAlt = "UK: My flight got held up for two hours, so I missed my connection.",
                arabicTranslation = "تأخرت رحلتي ساعتين، لذا فاتني القطار التالي.",
                expressionNoteAr = "'connecting train/flight' تعني الرحلة أو القطار المواصل للرحلة الأولى.",
                grammarInContextAr = "المبني للمجهول في الماضي البسيط (was delayed) مع أداة النتيجة (so).",
                realWorldUsageAr = "تُستخدم عند التحدث مع موظف المطار أو شرح سبب التأخر للزملاء.",
                similarExamples = listOf(
                    "Could you rebook me on the next available flight?" to "هل يمكنك إعادة حجزي على أقرب رحلة متاحة؟",
                    "Where do I collect my checked luggage?" to "أين أستلم أمتعتي المسجلة؟"
                ),
                comprehensionQuestionAr = "لماذا فات المتحدث القطار التالي؟",
                optionsAr = listOf("لأنه نسي التذكرة", "لأن رحلته الجوية تأخرت ساعتين", "لأن القطار أُلغي"),
                correctOptionIndex = 1
            ),
            SentenceLessonItem(
                id = "en_a2_2", language = TargetLanguage.EN, cefrLevel = CefrLevel.A2, unitNumber = 1, lessonNumber = 2,
                sentence = "I used to commute for an hour every day, but now I work remotely.",
                dialectVariantAlt = "UK: I used to have an hour's commute each way, though I work from home now.",
                arabicTranslation = "كنت أتنقل ساعة يوميًا للعمل، لكنني الآن أعمل عن بُعد.",
                expressionNoteAr = "'commute' تعني التنقل اليومي المنتظم بين المنزل ومقر العمل.",
                grammarInContextAr = "صيغة 'used to + فعل مجرد' تعبّر عن عادة كانت مستمرة في الماضي وتوقفت الآن.",
                realWorldUsageAr = "تُقال كثيرًا في المحادثات عن العمل ونمط الحياة المعاصر.",
                similarExamples = listOf(
                    "Working remotely saves me so much energy." to "العمل عن بُعد يوفر لي الكثير من الطاقة.",
                    "I still go to the office twice a month." to "ما زلت أذهب إلى المكتب مرتين شهريًا."
                ),
                comprehensionQuestionAr = "ماذا تفيد عبارة (used to commute)؟",
                optionsAr = listOf("ما زال يتنقل يوميًا", "عادة سابقة توقفت الآن", "خطة للمستقبل"),
                correctOptionIndex = 1
            ),
            SentenceLessonItem(
                id = "en_a2_3", language = TargetLanguage.EN, cefrLevel = CefrLevel.A2, unitNumber = 1, lessonNumber = 3,
                sentence = "Would you mind turning the volume down a little? I'm on a call.",
                dialectVariantAlt = "UK: Would you mind turning the telly down a bit? I'm on a call.",
                arabicTranslation = "هل تمانع في خفض الصوت قليلًا؟ أنا في مكالمة.",
                expressionNoteAr = "'Would you mind + V-ing' من أكثر أساليب الطلب لباقةً في الإنجليزية.",
                grammarInContextAr = "بعد (Would you mind) يأتي الفعل دائمًا مضافًا إليه (-ing).",
                realWorldUsageAr = "مثالية في المكتب أو المنزل عندما تطلب شيئًا بلطف دون إحراج.",
                similarExamples = listOf(
                    "Would you mind sending me the link again?" to "هل تمانع في إرسال الرابط لي مجددًا؟",
                    "Not at all, I'll lower it right away." to "لا أمانع أبدًا، سأخفضه فورًا."
                ),
                comprehensionQuestionAr = "ما الصيغة الصحيحة بعد Would you mind؟",
                optionsAr = listOf("الفعل مضافًا له ing", "الفعل في الماضي", "المصدر مع to"),
                correctOptionIndex = 0
            ),
            SentenceLessonItem(
                id = "en_a2_4", language = TargetLanguage.EN, cefrLevel = CefrLevel.A2, unitNumber = 1, lessonNumber = 4,
                sentence = "I ordered this laptop last week, yet it arrived with a cracked screen.",
                dialectVariantAlt = "UK: I ordered this laptop last week, yet it turned up with a cracked screen.",
                arabicTranslation = "طلبت هذا الحاسوب المحمول الأسبوع الماضي، ومع ذلك وصل بشاشة مكسورة.",
                expressionNoteAr = "'yet' هنا تربط بين جملتين متناقضتين بمعنى 'ومع ذلك' بشكل أنيق.",
                grammarInContextAr = "الماضي البسيط لسرد خطوات مشكلة حدثت بالفعل (ordered / arrived).",
                realWorldUsageAr = "تُستخدم عند التواصل مع خدمة العملاء وطلب استبدال منتج.",
                similarExamples = listOf(
                    "I'd like to request a full refund or a replacement." to "أود طلب استرداد كامل للمبلغ أو استبدال.",
                    "Here is my order confirmation number." to "إليك رقم تأكيد طلبي."
                ),
                comprehensionQuestionAr = "ما المشكلة التي يصفها المتحدث؟",
                optionsAr = listOf("تأخر الشحن فقط", "وصول الجهاز بشاشة مكسورة", "اختلاف اللون"),
                correctOptionIndex = 1
            )
        )
        CefrLevel.B1, CefrLevel.B2 -> listOf(
            SentenceLessonItem(
                id = "en_b_1", language = TargetLanguage.EN, cefrLevel = level, unitNumber = 1, lessonNumber = 1,
                sentence = "To be honest, I see your point, but we should weigh the long-term risks first.",
                dialectVariantAlt = "UK: Quite frankly, I take your point, though we ought to weigh up the risks.",
                arabicTranslation = "بصراحة، أتفهم وجهة نظرك، لكن علينا تقييم المخاطر طويلة المدى أولًا.",
                expressionNoteAr = "'see your point' تُظهر احترام رأي الطرف الآخر قبل طرح رأي مختلف بلباقة.",
                grammarInContextAr = "استخدام 'weigh the risks' كاستعارة مهنية شائعة (يوازن/يدرس المخاطر).",
                realWorldUsageAr = "تُستخدم في الاجتماعات والنقاشات المهنية لبناء حوار ناضج دون صدام.",
                similarExamples = listOf(
                    "Let's look at the numbers before jumping to conclusions." to "دعنا ننظر إلى الأرقام قبل التسرع في الاستنتاجات.",
                    "That's a fair concern, and we have a backup plan." to "هذا قلق وجيه، ولدينا خطة بديلة."
                ),
                comprehensionQuestionAr = "ما أثر استخدام (I see your point) في الحوار المهني؟",
                optionsAr = listOf("رفض الفكرة بحدة", "إبداء التفهم قبل تقديم تحفّظ مهذب", "إنهاء الاجتماع"),
                correctOptionIndex = 1
            ),
            SentenceLessonItem(
                id = "en_b_2", language = TargetLanguage.EN, cefrLevel = level, unitNumber = 1, lessonNumber = 2,
                sentence = "Had we launched the campaign a week earlier, we would have doubled our sign-ups.",
                dialectVariantAlt = "UK: Had we rolled out the campaign a week sooner, sign-ups would have doubled.",
                arabicTranslation = "لو كنا أطلقنا الحملة قبل أسبوع، لكنا ضاعفنا عدد المسجلين.",
                expressionNoteAr = "'Had we + تصريف ثالث' أسلوب شرطي ثالث رسمي وأنيق يعادل (If we had).",
                grammarInContextAr = "الشرط الثالث (Third Conditional) لتحليل نتائج افتراضية في الماضي.",
                realWorldUsageAr = "شائعة جدًا في تقييم المشاريع بعد التنفيذ ومقابلات العمل المتقدمة.",
                similarExamples = listOf(
                    "In hindsight, testing on mobile first made all the difference." to "بالنظر للماضي، الاختبار على الهاتف أولًا صنع الفارق كله.",
                    "We'll apply that lesson to next quarter's release." to "سنطبّق هذا الدرس في إطلاق الربع القادم."
                ),
                comprehensionQuestionAr = "على ماذا يدل التركيب (Had we launched...)?",
                optionsAr = listOf("سؤال عن موعد الإطلاق", "شرط افتراضي في الماضي لم يحدث", "أمر مباشر"),
                correctOptionIndex = 1
            ),
            SentenceLessonItem(
                id = "en_b_3", language = TargetLanguage.EN, cefrLevel = level, unitNumber = 1, lessonNumber = 3,
                sentence = "I've been juggling three tight deadlines, so I need to prioritize the client demo.",
                dialectVariantAlt = "UK: I've been juggling three tight deadlines, so the client demo takes priority.",
                arabicTranslation = "كنت أوازن بين ثلاثة مواعيد نهائية ضيقة، لذا أحتاج لإعطاء الأولوية لعرض العميل.",
                expressionNoteAr = "'juggling deadlines' تعبير اصطلاحي طبيعي يعني التعامل مع عدة مهام ضاغطة معًا.",
                grammarInContextAr = "المضارع التام المستمر (have been + V-ing) للتعبير عن ضغط مستمر حتى اللحظة.",
                realWorldUsageAr = "طبيعية جدًا في اجتماعات المتابعة اليومية (Stand-up meetings).",
                similarExamples = listOf(
                    "Can we push the internal review to Thursday?" to "هل يمكننا تأجيل المراجعة الداخلية إلى الخميس؟",
                    "Let's focus on what delivers the highest impact." to "دعنا نركز على ما يحقق أعلى أثر."
                ),
                comprehensionQuestionAr = "ما معنى (juggling three tight deadlines)؟",
                optionsAr = listOf("إلغاء ثلاثة اجتماعات", "إدارة عدة مواعيد ضاغطة في وقت واحد", "تأجيل المشروع شهرًا"),
                correctOptionIndex = 1
            ),
            SentenceLessonItem(
                id = "en_b_4", language = TargetLanguage.EN, cefrLevel = level, unitNumber = 1, lessonNumber = 4,
                sentence = "Despite the initial skepticism, the pilot program exceeded every benchmark.",
                dialectVariantAlt = "UK: Despite initial scepticism, the pilot scheme surpassed every benchmark.",
                arabicTranslation = "رغم التشكيك المبدئي، تجاوز البرنامج التجريبي جميع المعايير المرجعية.",
                expressionNoteAr = "لاحظ الفرق الإملائي: skepticism (أمريكية) مقابل scepticism (بريطانية).",
                grammarInContextAr = "بعد (Despite) يأتي اسم أو شبه جملة اسمية دون (of).",
                realWorldUsageAr = "مثالية في العروض التقديمية وتقارير النتائج والنجاحات.",
                similarExamples = listOf(
                    "The data clearly validates our original hypothesis." to "البيانات تؤكد بوضوح فرضيتنا الأصلية.",
                    "We are now ready to scale across the region." to "نحن مستعدون الآن للتوسع في المنطقة."
                ),
                comprehensionQuestionAr = "كيف كانت نتيجة البرنامج التجريبي؟",
                optionsAr = listOf("توقف مبكرًا", "تجاوز كل المعايير المتوقعة", "لم يحقق النتائج المطلوبة"),
                correctOptionIndex = 1
            )
        )
        CefrLevel.C1, CefrLevel.C2 -> listOf(
            SentenceLessonItem(
                id = "en_c_1", language = TargetLanguage.EN, cefrLevel = level, unitNumber = 1, lessonNumber = 1,
                sentence = "Not only does sustained immersion rewire comprehension, but it also sharpens pragmatic intuition.",
                dialectVariantAlt = "UK: Not only does sustained immersion reshape comprehension, it also hones pragmatic intuition.",
                arabicTranslation = "لا يعيد الانغماس المستمر تشكيل الفهم فحسب، بل يصقل أيضًا الحدس التداولي للغة.",
                expressionNoteAr = "'pragmatic intuition' تشير إلى الإحساس السياقي بما يليق قوله في الموقف الثقافي.",
                grammarInContextAr = "القلب النحوي (Inversion: Not only does...) لإضفاء قوة بلاغية عالية في C1–C2.",
                realWorldUsageAr = "تُستخدم في المقالات الأكاديمية والمحاضرات والخطابات المؤثرة.",
                similarExamples = listOf(
                    "Rarely have we observed such a pronounced shift in fluency." to "نادرًا ما لاحظنا تحولًا بهذا الوضوح في الطلاقة.",
                    "Nuance often separates competence from genuine mastery." to "الدقة في الفروق هي ما يفصل الكفاءة عن الإتقان الحقيقي."
                ),
                comprehensionQuestionAr = "لماذا جاء الفعل المساعد (does) قبل الفاعل بعد Not only؟",
                optionsAr = listOf("خطأ مطبعي", "قاعدة القلب النحوي للتوكيد البلاغي", "لتحويل الجملة إلى سؤال"),
                correctOptionIndex = 1
            ),
            SentenceLessonItem(
                id = "en_c_2", language = TargetLanguage.EN, cefrLevel = level, unitNumber = 1, lessonNumber = 2,
                sentence = "Rather than dismissing the critique out of hand, she reframed it as a catalyst for reform.",
                dialectVariantAlt = "UK: Rather than brushing aside the critique out of hand, she reframed it as a catalyst for reform.",
                arabicTranslation = "بدلًا من رفض النقد فورًا دون تمحيص، أعادت صياغته كمحفز للإصلاح.",
                expressionNoteAr = "'out of hand' تعبير متقدم يعني 'بشكل فوري دون أدنى تفكير أو نقاش'.",
                grammarInContextAr = "استخدام (Rather than + V-ing) للمقابلة الأسلوبية الرفيعة.",
                realWorldUsageAr = "تُستخدم في التحليل القيادي والنقد الإعلامي والكتابة الرفيعة.",
                similarExamples = listOf(
                    "His remarks struck a remarkably conciliatory tone." to "اتسمت تصريحاته بنبرة تصالحية لافتة.",
                    "A measured response defuses tension far better than haste." to "الرد المتزن ينزع فتيل التوتر أفضل بكثير من التسرع."
                ),
                comprehensionQuestionAr = "ماذا يعني تعبير (dismiss out of hand)؟",
                optionsAr = listOf("القبول السريع", "الرفض الفوري دون دراسة", "التأجيل لوقت لاحق"),
                correctOptionIndex = 1
            ),
            SentenceLessonItem(
                id = "en_c_3", language = TargetLanguage.EN, cefrLevel = level, unitNumber = 1, lessonNumber = 3,
                sentence = "The policy appears benign on paper, yet its unintended repercussions disproportionately affect small firms.",
                dialectVariantAlt = "UK: On paper the policy looks benign, yet its knock-on effects fall heavily on small businesses.",
                arabicTranslation = "تبدو السياسة غير ضارة على الورق، إلا أن تداعياتها غير المقصودة تؤثر بشكل غير متناسب على الشركات الصغيرة.",
                expressionNoteAr = "'on paper' تعني نظريًا مقارنة بالواقع العملي، و'knock-on effects' شائعة في البريطانية.",
                grammarInContextAr = "توظيف الظرف (disproportionately) للتعبير الدقيق عن عدم التكافؤ.",
                realWorldUsageAr = "أساسية في التحليل الاقتصادي والقانوني وكتابة مقالات IELTS/C2.",
                similarExamples = listOf(
                    "Empirical evidence paints a far more nuanced picture." to "الأدلة التجريبية ترسم صورة أكثر دقة وتفصيلًا.",
                    "Policy makers must anticipate second-order consequences." to "يجب على صانعي السياسات توقع النتائج غير المباشرة."
                ),
                comprehensionQuestionAr = "ما الفكرة الأساسية في الجملة؟",
                optionsAr = listOf("السياسة تدعم الشركات الصغيرة بقوة", "السياسة تبدو جيدة نظريًا لكنها تضر الشركات الصغيرة عمليًا", "السياسة لم تُكتب بعد"),
                correctOptionIndex = 1
            ),
            SentenceLessonItem(
                id = "en_c_4", language = TargetLanguage.EN, cefrLevel = level, unitNumber = 1, lessonNumber = 4,
                sentence = "Were the committee to adopt a phased rollout, resistance would likely dissipate.",
                dialectVariantAlt = "UK: Were the committee to opt for a phased rollout, pushback would probably subside.",
                arabicTranslation = "لو اعتمدت اللجنة إطلاقًا مرحليًا، فمن المرجح أن تتبدد المقاومة.",
                expressionNoteAr = "'phased rollout' تعني التنفيذ على مراحل مدروسة، و'dissipate' تتبدد تدريجيًا.",
                grammarInContextAr = "الشرط الثاني المقلوب الرسمي (Were + فاعل + to + مصدر) المميز لمستوى C2.",
                realWorldUsageAr = "مستخدمة في المذكرات التنفيذية والتوصيات الاستراتيجية رفيعة المستوى.",
                similarExamples = listOf(
                    "Consensus hinges on transparent communication from day one." to "يتوقف الإجماع على التواصل الشفاف منذ اليوم الأول.",
                    "Incremental milestones build institutional trust." to "المراحل التدريجية تبني الثقة المؤسسية."
                ),
                comprehensionQuestionAr = "ما الحل الذي تقترحه الجملة لتقليل الاعتراضات؟",
                optionsAr = listOf("إيقاف المشروع", "الإطلاق التدريجي على مراحل", "تغيير اللجنة"),
                correctOptionIndex = 1
            )
        )
    }

    private fun spanishBank(level: CefrLevel): List<SentenceLessonItem> = when (level) {
        CefrLevel.A1 -> listOf(
            SentenceLessonItem(
                id = "es_a1_1", language = TargetLanguage.ES, cefrLevel = CefrLevel.A1, unitNumber = 1, lessonNumber = 1,
                sentence = "¡Hola! Mucho gusto. ¿Me pones un café con leche para llevar, por favor?",
                dialectVariantAlt = "LATAM (es-MX): ¡Hola! Mucho gusto. ¿Me das un café con leche para llevar, por favor?",
                arabicTranslation = "مرحبًا! سررت بلقائك. هل تعطيني قهوة بالحليب سفري، من فضلك؟",
                expressionNoteAr = "في إسبانيا يُستخدم '¿Me pones...?' كثيرًا في المقاهي، بينما في أمريكا اللاتينية يشيع '¿Me das...?' أو '¿Me regala...?'.",
                grammarInContextAr = "استخدام الضمير (me) قبل الفعل للطلب الموجه للنادل بشكل طبيعي وودود.",
                realWorldUsageAr = "تُقال يوميًا في المقاهي في مدريد ومكسيكو سيتي وبوينس آيرس.",
                similarExamples = listOf(
                    "¿Cuánto es todo, por favor?" to "كم المبلغ الإجمالي، من فضلك؟",
                    "Quisiera un vaso de agua sin gas." to "أود كوب ماء بدون غاز."
                ),
                comprehensionQuestionAr = "ما الفرق اللهجي بين إسبانيا وأمريكا اللاتينية في طلب القهوة هنا؟",
                optionsAr = listOf("لا يوجد فرق أبدًا", "في إسبانيا يشيع Me pones وفي اللاتينية Me das", "القهوة لا تُسمى café"),
                correctOptionIndex = 1
            ),
            SentenceLessonItem(
                id = "es_a1_2", language = TargetLanguage.ES, cefrLevel = CefrLevel.A1, unitNumber = 1, lessonNumber = 2,
                sentence = "Perdone, ¿cómo puedo llegar a la estación de metro más cercana?",
                dialectVariantAlt = "LATAM (es-MX): Disculpe, ¿cómo llego a la estación del metro más cercana?",
                arabicTranslation = "عفوًا، كيف يمكنني الوصول إلى أقرب محطة مترو؟",
                expressionNoteAr = "'Perdone' شائعة في إسبانيا و'Disculpe' شائعة جدًا في المكسيك وأمريكا اللاتينية.",
                grammarInContextAr = "صيغة التفضيل (más cercana) تتبع الاسم وتوافقه في التأنيث لأن estación مؤنث.",
                realWorldUsageAr = "تُستخدم فورًا عند التنقل في المدن الناطقة بالإسبانية.",
                similarExamples = listOf(
                    "Sigue todo recto y gira a la derecha." to "امشِ للأمام مباشرة ثم انعطف يمينًا.",
                    "Está a cinco minutos andando." to "إنها على بُعد خمس دقائق سيرًا."
                ),
                comprehensionQuestionAr = "لماذا جاءت كلمة (cercana) مؤنثة؟",
                optionsAr = listOf("لأنها تصف كلمة estación المؤنثة", "لأنها جمع", "لأنها فعل"),
                correctOptionIndex = 0
            ),
            SentenceLessonItem(
                id = "es_a1_3", language = TargetLanguage.ES, cefrLevel = CefrLevel.A1, unitNumber = 1, lessonNumber = 3,
                sentence = "Tengo una reserva a nombre de Nadia para dos noches con desayuno.",
                dialectVariantAlt = "LATAM (es-MX): Tengo una reservación a nombre de Nadia por dos noches con desayuno.",
                arabicTranslation = "لدي حجز باسم نادية لمدة ليلتين مع الإفطار.",
                expressionNoteAr = "في إسبانيا تُقال 'reserva' وفي المكسيك يُستخدم أيضًا 'reservación'.",
                grammarInContextAr = "تعبير 'a nombre de' يعني 'باسم فلان'، وحرف الجر 'con' يعني 'مع'.",
                realWorldUsageAr = "عبارة أساسية عند تسجيل الدخول في الفنادق والشقق الفندقية.",
                similarExamples = listOf(
                    "¿Cuál es la contraseña del wifi?" to "ما كلمة مرور الواي فاي؟",
                    "¿A qué hora sirven el desayuno?" to "في أي ساعة يقدمون الإفطار؟"
                ),
                comprehensionQuestionAr = "ماذا يشمل الحجز في هذه الجملة؟",
                optionsAr = listOf("تذكرة طيران", "ليلتين مع الإفطار", "استئجار سيارة"),
                correctOptionIndex = 1
            ),
            SentenceLessonItem(
                id = "es_a1_4", language = TargetLanguage.ES, cefrLevel = CefrLevel.A1, unitNumber = 1, lessonNumber = 4,
                sentence = "¿Cuánto cuesta esta camisa y la tienen en talla mediana?",
                dialectVariantAlt = "LATAM (es-MX): ¿Cuánto cuesta esta camisa y la tienen en talla mediana?",
                arabicTranslation = "كم سعر هذا القميص وهل لديكم منه مقاس متوسط؟",
                expressionNoteAr = "الضمير (la) يعود على القميص (la camisa) لتجنب التكرار مثل المتحدثين الأصليين.",
                grammarInContextAr = "الفعل costar يتحول فيه حرف o إلى ue في المضارع (cuesta).",
                realWorldUsageAr = "تُستخدم يوميًا في المتاجر والأسواق.",
                similarExamples = listOf(
                    "¿Puedo probármela, por favor?" to "هل يمكنني قياسها، من فضلك؟",
                    "Prefiero pagarlo con tarjeta." to "أفضل دفعه بالبطاقة."
                ),
                comprehensionQuestionAr = "إلى ماذا يعود الضمير (la) في (la tienen)؟",
                optionsAr = listOf("إلى البائع", "إلى القميص (la camisa)", "إلى السعر"),
                correctOptionIndex = 1
            )
        )
        CefrLevel.A2 -> listOf(
            SentenceLessonItem(
                id = "es_a2_1", language = TargetLanguage.ES, cefrLevel = CefrLevel.A2, unitNumber = 1, lessonNumber = 1,
                sentence = "Mi vuelo se retrasó dos horas y por eso perdí el tren de conexión.",
                dialectVariantAlt = "LATAM (es-MX): Mi vuelo se demoró dos horas y por eso perdí la conexión.",
                arabicTranslation = "تأخرت رحلتي ساعتين ولهذا السبب فاتني قطار المواصلة.",
                expressionNoteAr = "'retrasarse' شائعة في إسبانيا و'demorarse' مألوفة جدًا في أمريكا اللاتينية.",
                grammarInContextAr = "الماضي البسيط (Pretérito Indefinido: se retrasó / perdí) للأحداث المكتملة في الماضي.",
                realWorldUsageAr = "مهمة جدًا في المطارات ومحطات القطارات عند تعديل الحجوزات.",
                similarExamples = listOf(
                    "¿Pueden cambiarme al próximo vuelo disponible?" to "هل يمكنكم نقلي للرحلة المتاحة التالية؟",
                    "Mi maleta todavía no ha salido." to "حقيبتي لم تخرج بعد."
                ),
                comprehensionQuestionAr = "ما زمن الفعل (perdí) في هذه الجملة؟",
                optionsAr = listOf("المستقبل", "الماضي البسيط المكتمل", "الأمر"),
                correctOptionIndex = 1
            ),
            SentenceLessonItem(
                id = "es_a2_2", language = TargetLanguage.ES, cefrLevel = CefrLevel.A2, unitNumber = 1, lessonNumber = 2,
                sentence = "Antes tardaba una hora en llegar a la oficina, pero ahora trabajo desde casa.",
                dialectVariantAlt = "LATAM (es-MX): Antes me hacía una hora a la oficina, pero ahora trabajo remoto.",
                arabicTranslation = "سابقًا كنت أستغرق ساعة للوصول إلى المكتب، لكنني الآن أعمل من المنزل.",
                expressionNoteAr = "الفعل 'tardar + مدة + en + مصدر' يُستخدم للتعبير عن الوقت المستغرق في فعل شيء.",
                grammarInContextAr = "الماضي الناقص (tardaba) لوصف عادة متكررة في الماضي مقابل المضارع الآن.",
                realWorldUsageAr = "تُستخدم للحديث عن روتين العمل السابق والحالي.",
                similarExamples = listOf(
                    "Aprovecho el tiempo libre para estudiar español." to "أستغل وقت الفراغ لدراسة الإسبانية.",
                    "Solo voy a la oficina los martes." to "أذهب للمكتب أيام الثلاثاء فقط."
                ),
                comprehensionQuestionAr = "لماذا استُخدم زمن (tardaba) هنا؟",
                optionsAr = listOf("لحدث وقع مرة واحدة", "لوصف عادة متكررة في الماضي", "لطلب مهذب"),
                correctOptionIndex = 1
            ),
            SentenceLessonItem(
                id = "es_a2_3", language = TargetLanguage.ES, cefrLevel = CefrLevel.A2, unitNumber = 1, lessonNumber = 3,
                sentence = "¿Te importaría bajar un poco el volumen? Estoy en una llamada importante.",
                dialectVariantAlt = "LATAM (es-MX): ¿Te molestaría bajarle tantito al volumen? Estoy en una llamada.",
                arabicTranslation = "هل يزعجك خفض الصوت قليلًا؟ أنا في مكالمة مهمة.",
                expressionNoteAr = "'¿Te importaría + مصدر?' أسلوب راقٍ جدًا للطلب المهذب في الإسبانية.",
                grammarInContextAr = "استخدام الشرطي (importaría) لتلطيف الطلب وجعله لبقًا.",
                realWorldUsageAr = "مناسبة في بيئة العمل والمنزل ومع الزملاء.",
                similarExamples = listOf(
                    "Claro, perdona, ahora mismo lo bajo." to "بالطبع، عفوًا، سأخفضه فورًا.",
                    "¿Podrías enviarme el archivo antes de las cinco?" to "هل يمكنك إرسال الملف لي قبل الخامسة؟"
                ),
                comprehensionQuestionAr = "ما وظيفة صيغة (¿Te importaría...?)؟",
                optionsAr = listOf("طلب مهذب جدًا", "أمر صارم", "اعتذار"),
                correctOptionIndex = 0
            ),
            SentenceLessonItem(
                id = "es_a2_4", language = TargetLanguage.ES, cefrLevel = CefrLevel.A2, unitNumber = 1, lessonNumber = 4,
                sentence = "Compré este ordenador la semana pasada, pero llegó con la pantalla rota.",
                dialectVariantAlt = "LATAM (es-MX): Compré esta computadora la semana pasada, pero llegó con la pantalla rota.",
                arabicTranslation = "اشتريت هذا الحاسوب الأسبوع الماضي، لكنه وصل بشاشة مكسورة.",
                expressionNoteAr = "لاحظ الفرق الشهير: 'ordenador' في إسبانيا مقابل 'computadora' في أمريكا اللاتينية!",
                grammarInContextAr = "الصفة 'rota' (مكسورة) تطابق الاسم المؤنث 'la pantalla'.",
                realWorldUsageAr = "تُستخدم عند التبليغ عن منتج تالف وطلب استبداله.",
                similarExamples = listOf(
                    "Me gustaría solicitar un reembolso completo." to "أود طلب استرداد كامل للمبلغ.",
                    "Aquí tienes el número de mi pedido." to "إليك رقم طلبي."
                ),
                comprehensionQuestionAr = "ماذا يُسمى الحاسوب في أمريكا اللاتينية؟",
                optionsAr = listOf("ordenador", "computadora", "pantalla"),
                correctOptionIndex = 1
            )
        )
        CefrLevel.B1, CefrLevel.B2 -> listOf(
            SentenceLessonItem(
                id = "es_b_1", language = TargetLanguage.ES, cefrLevel = level, unitNumber = 1, lessonNumber = 1,
                sentence = "Entiendo tu postura, pero conviene que analicemos los riesgos antes de decidir.",
                dialectVariantAlt = "LATAM (es-MX): Entiendo tu punto, pero conviene que analicemos los riesgos antes de decidir.",
                arabicTranslation = "أتفهم موقفك، لكن من الأفضل أن نحلل المخاطر قبل اتخاذ القرار.",
                expressionNoteAr = "'conviene que + Subjuntivo' صيغة مهنية أنيقة للتوصية المدروسة.",
                grammarInContextAr = "تفعيل صيغة المنصوب (Subjuntivo: analicemos) بعد تعبيرات التقييم (conviene que).",
                realWorldUsageAr = "تُستخدم في الاجتماعات والنقاشات المهنية بهدوء واحترافية.",
                similarExamples = listOf(
                    "Es imprescindible que revisemos el presupuesto." to "من الضروري جدًا أن نراجع الميزانية.",
                    "Valoro mucho tu aportación al equipo." to "أقدّر كثيرًا مساهمتك في الفريق."
                ),
                comprehensionQuestionAr = "لماذا جاء الفعل (analicemos) في صيغة Subjuntivo؟",
                optionsAr = listOf("لأنه في الماضي", "لأنه جاء بعد تعبير توصية (conviene que)", "لأنه اسم"),
                correctOptionIndex = 1
            ),
            SentenceLessonItem(
                id = "es_b_2", language = TargetLanguage.ES, cefrLevel = level, unitNumber = 1, lessonNumber = 2,
                sentence = "Si hubiéramos lanzado la campaña antes, habríamos duplicado los resultados.",
                dialectVariantAlt = "LATAM (es-MX): Si hubiéramos lanzado la campaña antes, habríamos duplicado los resultados.",
                arabicTranslation = "لو كنا أطلقنا الحملة أبكر، لكنا ضاعفنا النتائج.",
                expressionNoteAr = "الشرط المركب الماضي للتفكير النقدي وتقييم المشاريع بعد تنفيذها.",
                grammarInContextAr = "Pluscuamperfecto de subjuntivo (hubiéramos lanzado) + Condicional compuesto (habríamos duplicado).",
                realWorldUsageAr = "ممتازة في تقارير العمل ومقابلات التوظيف المتقدمة.",
                similarExamples = listOf(
                    "De haberlo sabido, habríamos reservado con antelación." to "لو علمنا بذلك، لحجزنا مسبقًا.",
                    "Lo tendremos en cuenta para la próxima etapa." to "سنضع ذلك في الاعتبار للمرحلة القادمة."
                ),
                comprehensionQuestionAr = "ماذا تعبّر الجملة الشرطية هنا؟",
                optionsAr = listOf("حقيقة حالية", "احتمال مستقبلي", "نتيجة افتراضية في الماضي لم تتحقق"),
                correctOptionIndex = 2
            ),
            SentenceLessonItem(
                id = "es_b_3", language = TargetLanguage.ES, cefrLevel = level, unitNumber = 1, lessonNumber = 3,
                sentence = "Aunque el proyecto parecía complejo al principio, logramos entregarlo a tiempo.",
                dialectVariantAlt = "LATAM (es-MX): Aunque el proyecto parecía difícil al inicio, logramos entregarlo a tiempo.",
                arabicTranslation = "رغم أن المشروع بدا معقدًا في البداية، تمكنا من تسليمه في الوقت المحدد.",
                expressionNoteAr = "'lograr + مصدر' تعبّر عن النجاح بعد مجهود، وهي أقوى دلاليًا من مجرد poder.",
                grammarInContextAr = "جاءت (Aunque) مع الدلالي (parecía) لأنها تصف حقيقة واقعة في الماضي.",
                realWorldUsageAr = "مفيدة جدًا عند عرض إنجازاتك المهنية.",
                similarExamples = listOf(
                    "La coordinación entre equipos fue clave." to "كان التنسيق بين الفرق عاملًا حاسمًا.",
                    "Cumplimos todos los objetivos sin exceder el coste." to "حققنا كل الأهداف دون تجاوز التكلفة."
                ),
                comprehensionQuestionAr = "ماذا يضيف الفعل (lograr) لمعنى الجملة؟",
                optionsAr = listOf("الفشل", "النجاح في تحقيق هدف بعد جهد", "التردد"),
                correctOptionIndex = 1
            ),
            SentenceLessonItem(
                id = "es_b_4", language = TargetLanguage.ES, cefrLevel = level, unitNumber = 1, lessonNumber = 4,
                sentence = "Por mucho que insistieran, no estaba dispuesto a comprometer la calidad.",
                dialectVariantAlt = "LATAM (es-MX): Por más que insistieran, no estaba dispuesto a sacrificar la calidad.",
                arabicTranslation = "مهما أصرّوا، لم يكن مستعدًا للتنازل عن الجودة.",
                expressionNoteAr = "'Por mucho que + Subjuntivo' تعبير متقدم يعني 'مهما حدث/مهما فعلوا'.",
                grammarInContextAr = "استخدام (estar dispuesto a) للتعبير عن الاستعداد أو عدمه.",
                realWorldUsageAr = "شائعة في التفاوض وإدارة المنتجات واتخاذ القرارات الحاسمة.",
                similarExamples = listOf(
                    "La calidad a largo plazo genera fidelidad." to "الجودة طويلة الأمد تبني الولاء.",
                    "Preferimos avanzar con paso firme." to "نفضّل التقدم بخطى ثابتة."
                ),
                comprehensionQuestionAr = "ما موقف الشخص في الجملة تجاه الجودة؟",
                optionsAr = listOf("وافق على تقليلها", "رفض التنازل عنها مهما كان الضغط", "لم يهتم بها"),
                correctOptionIndex = 1
            )
        )
        CefrLevel.C1, CefrLevel.C2 -> listOf(
            SentenceLessonItem(
                id = "es_c_1", language = TargetLanguage.ES, cefrLevel = level, unitNumber = 1, lessonNumber = 1,
                sentence = "Lejos de descartar la crítica de plano, la convirtió en un motor de transformación.",
                dialectVariantAlt = "LATAM (es-MX): Lejos de rechazar la crítica de tajo, la transformó en un impulso de cambio.",
                arabicTranslation = "بدلًا من رفض النقد جملةً وتفصيلًا، حوّلته إلى محرك للتحول.",
                expressionNoteAr = "'de plano' (إسبانية عامة) و'de tajo' (لاتينية) تعنيان الرفض القاطع الفوري.",
                grammarInContextAr = "التركيب البلاغي (Lejos de + مصدر) يرفع مستوى الأسلوب في C1–C2.",
                realWorldUsageAr = "مثالية في المقالات الفكرية والخطابات القيادية والامتحانات العليا DELE C1/C2.",
                similarExamples = listOf(
                    "Su intervención marcó un punto de inflexión en el debate." to "شكّلت مداخلته نقطة تحول في النقاش.",
                    "Matizar las ideas demuestra rigor intelectual." to "تدقيق الفروق في الأفكار يدل على رصانة فكرية."
                ),
                comprehensionQuestionAr = "ماذا يفيد التركيب (Lejos de descartar...)?",
                optionsAr = listOf("البعد الجغرافي", "نفي الرفض وتقديم موقف معاكس إيجابي", "الشك"),
                correctOptionIndex = 1
            ),
            SentenceLessonItem(
                id = "es_c_2", language = TargetLanguage.ES, cefrLevel = level, unitNumber = 1, lessonNumber = 2,
                sentence = "Cabe destacar que la inmersión diaria afina la intuición pragmática del hablante.",
                dialectVariantAlt = "LATAM (es-MX): Vale la pena subrayar que la inmersión diaria afina la intuición pragmática.",
                arabicTranslation = "تجدر الإشارة إلى أن الانغماس اليومي يصقل الحدس التداولي للمتحدث.",
                expressionNoteAr = "'Cabe destacar que' من أرقى الروابط الأكاديمية والصحفية في الإسبانية.",
                grammarInContextAr = "الفعل caber في صيغة غير شخصية (cabe + مصدر) لتسليط الضوء على فكرة محورية.",
                realWorldUsageAr = "تُستخدم في الأبحاث والتقارير التنفيذية والعروض الأكاديمية.",
                similarExamples = listOf(
                    "No obstante, conviene contrastar diversas fuentes." to "ومع ذلك، ينبغي مقارنة مصادر متعددة.",
                    "El dominio real se percibe en la naturalidad." to "يظهر الإتقان الحقيقي في العفوية الطبيعية."
                ),
                comprehensionQuestionAr = "ما معنى العبارة الرسمية (Cabe destacar que)؟",
                optionsAr = listOf("من المستحيل أن", "تجدر الإشارة إلى أن", "في الختام فقط"),
                correctOptionIndex = 1
            ),
            SentenceLessonItem(
                id = "es_c_3", language = TargetLanguage.ES, cefrLevel = level, unitNumber = 1, lessonNumber = 3,
                sentence = "A simple vista la medida parece inocua, pero sus efectos colaterales son profundos.",
                dialectVariantAlt = "LATAM (es-MX): A primera vista la medida parece inofensiva, pero sus consecuencias son profundas.",
                arabicTranslation = "للوهلة الأولى يبدو الإجراء غير ضار، لكن آثاره الجانبية عميقة.",
                expressionNoteAr = "'A simple vista' تعني بالنظرة السطحية الأولى قبل التعمق.",
                grammarInContextAr = "توظيف صفات دقيقة (inocua / colaterales) يميز كتابة ومحادثة C2.",
                realWorldUsageAr = "تُستخدم في التحليل الاقتصادي والسياسي والاجتماعي.",
                similarExamples = listOf(
                    "Los datos empíricos matizan esa conclusión inicial." to "البيانات الميدانية تضبط ذلك الاستنتاج الأولي.",
                    "Conviene prever las repercusiones a mediano plazo." to "ينبغي توقع التداعيات على المدى المتوسط."
                ),
                comprehensionQuestionAr = "ما معنى (inocua) في هذا السياق؟",
                optionsAr = listOf("مكلفة جدًا", "غير ضارة", "سريعة جدًا"),
                correctOptionIndex = 1
            ),
            SentenceLessonItem(
                id = "es_c_4", language = TargetLanguage.ES, cefrLevel = level, unitNumber = 1, lessonNumber = 4,
                sentence = "De haberse implementado por fases, la transición habría suscitado mucho menos recelo.",
                dialectVariantAlt = "LATAM (es-MX): De haberse aplicado por etapas, la transición habría generado menos desconfianza.",
                arabicTranslation = "لو نُفّذت على مراحل، لكان الانتقال قد أثار قدرًا أقل بكثير من التوجس.",
                expressionNoteAr = "'De + مصدر مركب (haberse implementado)' بديل بلاغي رفيع لجملة الشرط (Si se hubiera...).",
                grammarInContextAr = "صيغة المبني للمجهول الانعكاسي في المصدر المركب مع الشرط التام.",
                realWorldUsageAr = "ذروة التعبير الأكاديمي والمهني في مستوى C2.",
                similarExamples = listOf(
                    "La transparencia disipa la incertidumbre institucional." to "الشفافية تبدّد حالة عدم اليقين المؤسسي.",
                    "El consenso duradero exige escucha activa." to "الإجماع المستدام يتطلب إنصاتًا فاعلًا."
                ),
                comprehensionQuestionAr = "ماذا يعادل التركيب (De haberse implementado)?",
                optionsAr = listOf("بعد أن نُفّذت", "لو كانت قد نُفّذت", "لكي تُنفّذ"),
                correctOptionIndex = 1
            )
        )
    }

    val tutorScenarios: List<TutorScenario> = listOf(
        TutorScenario(
            id = "free_chat",
            titleAr = "محادثة حرة ذكية",
            subtitleAr = "تحدث بحرية في أي موضوع مع تصحيح فوري",
            roleInstructionEn = "You are a warm, adaptive native English tutor having a natural free conversation with an Arabic speaker.",
            roleInstructionEs = "Eres un tutor nativo de español cálido y adaptable conversando libremente con un estudiante arabófono.",
            starterEn = "Hey! Great to see you today. What's something interesting that happened in your day?",
            starterEs = "¡Hola! Qué gusto saludarte hoy. ¿Qué ha pasado de interesante en tu día?"
        ),
        TutorScenario(
            id = "airport",
            titleAr = "في المطار",
            subtitleAr = "إجراءات السفر، البوابات، وتأخير الرحلات",
            roleInstructionEn = "Act as an airport check-in and gate agent helping a traveler through boarding and flight changes.",
            roleInstructionEs = "Actúa como agente de aeropuerto ayudando a un viajero con el embarque y cambios de vuelo.",
            starterEn = "Good afternoon! May I see your passport and booking reference? Are you checking any bags today?",
            starterEs = "¡Buenas tardes! ¿Me permite su pasaporte y código de reserva? ¿Va a facturar equipaje hoy?"
        ),
        TutorScenario(
            id = "hotel",
            titleAr = "في الفندق",
            subtitleAr = "الحجز، طلبات الغرفة، وحل المشكلات",
            roleInstructionEn = "Act as a boutique hotel receptionist assisting a guest with check-in and room requests.",
            roleInstructionEs = "Actúa como recepcionista de hotel ayudando a un huésped con su registro y peticiones.",
            starterEn = "Welcome to Grand Horizon Hotel! Do you have a reservation with us tonight?",
            starterEs = "¡Bienvenido al Hotel Horizonte! ¿Tiene una reserva con nosotros para esta noche?"
        ),
        TutorScenario(
            id = "restaurant",
            titleAr = "في المطعم",
            subtitleAr = "طلب الطعام، الحساسية، والحساب",
            roleInstructionEn = "Act as a friendly restaurant waiter taking orders and recommending local dishes.",
            roleInstructionEs = "Actúa como camarero amable tomando el pedido y recomendando platos locales.",
            starterEn = "Welcome! Would you like to start with something to drink while you look over the menu?",
            starterEs = "¡Bienvenidos! ¿Les gustaría empezar con alguna bebida mientras revisan la carta?"
        ),
        TutorScenario(
            id = "job_interview",
            titleAr = "مقابلة عمل",
            subtitleAr = "أسئلة التوظيف، نقاط القوة، والتفاوض",
            roleInstructionEn = "Act as a hiring manager conducting a realistic, supportive professional job interview.",
            roleInstructionEs = "Actúa como responsable de selección realizando una entrevista laboral realista y constructiva.",
            starterEn = "Thanks for joining us today. To start off, could you walk me through your background and what excites you about this role?",
            starterEs = "Gracias por acompañarnos hoy. Para empezar, ¿podrías contarme tu experiencia y qué te motiva de este puesto?"
        ),
        TutorScenario(
            id = "shopping",
            titleAr = "التسوق والأسواق",
            subtitleAr = "المقاسات، الأسعار، والاستبدال",
            roleInstructionEn = "Act as a store associate helping a shopper find sizes, compare items, and check return policies.",
            roleInstructionEs = "Actúa como dependiente ayudando a un cliente con tallas, precios y devoluciones.",
            starterEn = "Hi there! Let me know if you're looking for a particular size or style today.",
            starterEs = "¡Hola! Avísame si buscas alguna talla o estilo en especial hoy."
        ),
        TutorScenario(
            id = "university",
            titleAr = "في الجامعة",
            subtitleAr = "النقاش الأكاديمي، الأبحاث، والمحاضرات",
            roleInstructionEn = "Act as a university advisor and seminar professor discussing study goals and research topics.",
            roleInstructionEs = "Actúa como profesor universitario conversando sobre estudios, seminarios e investigación.",
            starterEn = "Welcome to office hours! Which course topic or assignment would you like to unpack today?",
            starterEs = "¡Bienvenido a tutoría! ¿Qué tema del curso o proyecto te gustaría analizar hoy?"
        ),
        TutorScenario(
            id = "meetings",
            titleAr = "الاجتماعات المهنية",
            subtitleAr = "إدارة النقاش، الخطط، والاتفاق على المهام",
            roleInstructionEn = "Act as a project lead in a team strategy meeting discussing timelines, trade-offs, and deliverables.",
            roleInstructionEs = "Actúa como líder de proyecto en una reunión estratégica discutiendo plazos y entregables.",
            starterEn = "Thanks everyone for hopping on. What's the top priority we should lock in for this week's sprint?",
            starterEs = "Gracias por conectarse. ¿Cuál es la prioridad principal que debemos cerrar esta semana?"
        )
    )

    fun examQuestionsFor(language: TargetLanguage, level: CefrLevel): List<ExamQuestion> {
        val isEn = language == TargetLanguage.EN
        return listOf(
            ExamQuestion(
                id = "q_listen_1",
                skill = SkillType.LISTENING,
                promptAr = "استمع للجملة ثم اختر المعنى الدقيق في السياق:",
                stimulusTarget = if (isEn) {
                    when (level) {
                        CefrLevel.A1, CefrLevel.A2 -> "I'm afraid the morning train is sold out, but there's an express one at ten fifteen."
                        CefrLevel.B1, CefrLevel.B2 -> "We'll have to postpone the launch unless the mobile checkout bug is patched by noon."
                        else -> "While the forecast sounds optimistic, several understated variables could derail the timeline."
                    }
                } else {
                    when (level) {
                        CefrLevel.A1, CefrLevel.A2 -> "Me temo que el tren de la mañana está lleno, pero sale uno rápido a las diez y cuarto."
                        CefrLevel.B1, CefrLevel.B2 -> "Tendremos que posponer el lanzamiento a menos que corrijan el error antes del mediodía."
                        else -> "Aunque el pronóstico parezca optimista, varios factores sutiles podrían alterar el calendario."
                    }
                },
                options = when (level) {
                    CefrLevel.A1, CefrLevel.A2 -> listOf(
                        "القطار الصباحي ممتلئ ويوجد قطار سريع الساعة 10:15",
                        "تم إلغاء جميع القطارات اليوم",
                        "القطار السريع يغادر الساعة 10:50"
                    )
                    CefrLevel.B1, CefrLevel.B2 -> listOf(
                        "تم الإطلاق بنجاح قبل الظهر",
                        "سيُؤجَّل الإطلاق ما لم يُصلَح خطأ الدفع قبل الظهر",
                        "لا توجد أي مشاكل تقنية في التطبيق"
                    )
                    else -> listOf(
                        "التوقعات متشائمة للغاية",
                        "رغم التفاؤل الظاهري، هناك متغيرات خفية قد تعرقل الجدول الزمني",
                        "الجدول الزمني مضمون بنسبة 100%"
                    )
                },
                correctIndex = when (level) {
                    CefrLevel.A1, CefrLevel.A2 -> 0
                    else -> 1
                }
            ),
            ExamQuestion(
                id = "q_read_1",
                skill = SkillType.READING,
                promptAr = "اقرأ الفقرة القصيرة واستنتج الفكرة الصحيحة:",
                stimulusTarget = if (isEn) {
                    when (level) {
                        CefrLevel.A1, CefrLevel.A2 -> "Nora walks to the market every Saturday morning because fresh fruit is cheaper before nine o'clock."
                        CefrLevel.B1, CefrLevel.B2 -> "Switching to four-day workweeks raised staff retention, yet customer support still required staggered shifts."
                        else -> "The editorial argues that algorithmic curation narrows curiosity unless readers intentionally seek dissenting perspectives."
                    }
                } else {
                    when (level) {
                        CefrLevel.A1, CefrLevel.A2 -> "Nora camina al mercado cada sábado por la mañana porque la fruta fresca es más barata antes de las nueve."
                        CefrLevel.B1, CefrLevel.B2 -> "La semana laboral de cuatro días mejoró la retención del equipo, pero soporte siguió necesitando turnos escalonados."
                        else -> "El editorial sostiene que la curación algorítmica reduce la curiosidad salvo que el lector busque posturas distintas."
                    }
                },
                options = when (level) {
                    CefrLevel.A1, CefrLevel.A2 -> listOf(
                        "تذهب نورة مساءً لأن السوق يفتح ليلًا",
                        "تذهب نورة صباح السبت مبكرًا لأن الفاكهة الطازجة أرخص قبل التاسعة",
                        "نورة لا تحب شراء الفاكهة"
                    )
                    CefrLevel.B1, CefrLevel.B2 -> listOf(
                        "خفضت أربعة أيام عمل من رضا الموظفين",
                        "زادت احتفاظ الموظفين مع بقاء الحاجة لمناوبات متدرجة في الدعم",
                        "أُغلق قسم خدمة العملاء بالكامل"
                    )
                    else -> listOf(
                        "الخوارزميات توسّع الفضول تلقائيًا",
                        "الاختيار الخوارزمي يضيّق الفضول ما لم يسعَ القارئ عمدًا لوجهات نظر مغايرة",
                        "القراء لا يحتاجون لمصادر متنوعة"
                    )
                },
                correctIndex = 1
            )
        ) + ExpandedCurriculum.examQuestionsFor(language, level)
    }
}
