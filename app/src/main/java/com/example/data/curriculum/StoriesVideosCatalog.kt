package com.example.data.curriculum

import com.example.data.model.CefrLevel
import com.example.data.model.Dialect
import com.example.data.model.InteractiveStory
import com.example.data.model.StoryChoice
import com.example.data.model.StoryTurn
import com.example.data.model.TargetLanguage
import com.example.data.model.TimedSubtitleLine
import com.example.data.model.VideoLesson

object StoriesVideosCatalog {

    val stories: List<InteractiveStory> = listOf(
        InteractiveStory(
            id = "story_en_airport_mystery",
            language = TargetLanguage.EN,
            cefrLevel = CefrLevel.A2,
            genreAr = "سفر وغموض",
            titleAr = "الحقيبة المتشابهة في مطار هيثرو",
            subtitleTarget = "The Identical Suitcase at Heathrow",
            turns = listOf(
                StoryTurn("سارة (Sara)", 1.12f, "Wait a second! Excuse me, I think you accidentally picked up my navy suitcase.", "انتظر لحظة! عفوًا، أعتقد أنك أخذت حقيبتي الكحلية بالخطأ."),
                StoryTurn("دانيال (Daniel)", 0.88f, "Oh, let me check the luggage tag! You're right — mine has a yellow ribbon on the handle.", "أوه، دعني أتحقق من بطاقة الأمتعة! معك حق — حقيبتي عليها شريط أصفر على المقبض."),
                StoryTurn("سارة (Sara)", 1.12f, "What a relief! My laptop and conference notes are inside. Are you heading into central London too?", "يا للارتياح! حاسوبي وملاحظات المؤتمر بداخلها. هل تتجه إلى وسط لندن أيضًا؟")
            ),
            choices = listOf(
                StoryChoice(
                    replyText = "Yes, I'm taking the Heathrow Express. Would you like to walk to the platform together?",
                    arabicHint = "عرض ودود للمشي سويًا نحو رصيف القطار السريع",
                    outcomeSpeaker = "سارة (Sara)",
                    outcomeReply = "That sounds wonderful! By the way, I'm presenting on sustainable cities tomorrow morning.",
                    outcomeArabic = "يبدو ذلك رائعًا! بالمناسبة، سأقدم عرضًا عن المدن المستدامة صباح الغد.",
                    feedbackAr = "اختيار طبيعي ومهذب جدًا! أدّى إلى تعارف مهني مميز وتطور إيجابي في القصة."
                ),
                StoryChoice(
                    replyText = "Actually, I need to wait at the lost-baggage desk first because my suitcase still hasn't appeared.",
                    arabicHint = "الانتظار عند مكتب الأمتعة المفقودة لأن حقيبتك لم تظهر بعد",
                    outcomeSpeaker = "سارة (Sara)",
                    outcomeReply = "Oh no! Let me stay five minutes to help you describe it — I saw a bag with a yellow ribbon near Carousel 4!",
                    outcomeArabic = "يا إلهي! دعني أبقى خمس دقائق لأساعدك في وصفها — لقد رأيت حقيبة بشريط أصفر قرب السير 4!",
                    feedbackAr = "اختيار واقعي ومفيد! غيّر مسار القصة إلى مغامرة سريعة للعثور على حقيبتك."
                )
            ),
            comprehensionQuestionAr = "كيف تأكد دانيال أن الحقيبة ليست له؟",
            comprehensionOptionsAr = listOf("من لون الحقيبة الأحمر", "من بطاقة الأمتعة وعدم وجود الشريط الأصفر", "من وزنها الخفيف"),
            correctOptionIndex = 1
        ),
        InteractiveStory(
            id = "story_en_cafe_interview",
            language = TargetLanguage.EN,
            cefrLevel = CefrLevel.B1,
            genreAr = "عمل ومواقف اجتماعية",
            titleAr = "لقاء غير متوقع قبل المقابلة",
            subtitleTarget = "An Unexpected Chat Before the Interview",
            turns = listOf(
                StoryTurn("كريم (Karim)", 0.92f, "Sorry, is this seat taken? Every other table in the café is packed.", "عفوًا، هل هذا المقعد محجوز؟ كل الطاولات الأخرى في المقهى ممتلئة."),
                StoryTurn("كلير (Claire)", 1.15f, "Not at all, go right ahead! You look a bit nervous — big day ahead?", "لا أبدًا، تفضل! تبدو متوترًا قليلًا — هل أمامك يوم مهم؟"),
                StoryTurn("كريم (Karim)", 0.92f, "I have a product design interview across the street in twenty minutes, and I want to make a strong first impression.", "لدي مقابلة تصميم منتجات في المبنى المقابل بعد عشرين دقيقة، وأريد ترك انطباع أول قوي.")
            ),
            choices = listOf(
                StoryChoice(
                    replyText = "Could I quickly pitch my favorite project to you to warm up? I'd love honest feedback.",
                    arabicHint = "طلب ذكي لتجربة عرض مشروعك سريعًا كإحماء",
                    outcomeSpeaker = "كلير (Claire)",
                    outcomeReply = "I'd love to hear it! Funny coincidence — I actually lead the design team across the street. You're off to a great start!",
                    outcomeArabic = "يسعدني سماعه! مصادفة طريفة — أنا في الواقع أقود فريق التصميم في المبنى المقابل. بدايتك رائعة!",
                    feedbackAr = "مفاجأة رائعة في القصة! المبادرة المهذبة كشفت أن كلير هي مديرة الفريق نفسها."
                ),
                StoryChoice(
                    replyText = "Thanks! I'm just reviewing a couple of questions about how their mobile app handles accessibility.",
                    arabicHint = "التركيز الهادئ على مراجعة أسئلة ذكية حول إمكانية الوصول",
                    outcomeSpeaker = "كلير (Claire)",
                    outcomeReply = "Asking thoughtful questions about accessibility will definitely make you stand out. Best of luck!",
                    outcomeArabic = "طرح أسئلة مدروسة حول إمكانية الوصول سيجعلك تتميز بالتأكيد. كل التوفيق!",
                    feedbackAr = "رد احترافي وواثق يُظهر استعدادك العميق قبل المقابلة."
                )
            ),
            comprehensionQuestionAr = "لماذا كان كريم متوترًا في المقهى؟",
            comprehensionOptionsAr = listOf("لأنه أضاع هاتفه", "لأن لديه مقابلة عمل بعد عشرين دقيقة", "لأن المقهى مغلق"),
            correctOptionIndex = 1
        ),
        InteractiveStory(
            id = "story_es_madrid_cafe",
            language = TargetLanguage.ES,
            cefrLevel = CefrLevel.A1,
            genreAr = "الحياة اليومية والكوميديا",
            titleAr = "المفاجأة اللطيفة في مقهى مدريد",
            subtitleTarget = "Una sorpresa en un café de Madrid",
            turns = listOf(
                StoryTurn("ماتيو (Mateo)", 0.9f, "¡Buenos días! ¿Qué desea tomar hoy? Tenemos churros recién hechos.", "صباح الخير! ماذا تود أن تتناول اليوم؟ لدينا تشوروس طازج للتو."),
                StoryTurn("لينا (Lina)", 1.15f, "¡Qué bien huelen! Quisiera un chocolate caliente y dos churros, por favor.", "ما أطيب رائحتها! أود شوكولاتة ساخنة وقطعتي تشوروس، من فضلك."),
                StoryTurn("ماتيو (Mateo)", 0.9f, "¡Perfecto! Por cierto, hoy celebramos el aniversario del café y el postre incluye una sorpresa.", "ممتاز! بالمناسبة، نحتفل اليوم بذكرى تأسيس المقهى والتحلية تتضمن مفاجأة.")
            ),
            choices = listOf(
                StoryChoice(
                    replyText = "¡Enhorabuena! ¿De qué trata la sorpresa? Me encanta probar cosas nuevas.",
                    arabicHint = "تهنئة لطيفة مع فضول لتجربة المفاجأة",
                    outcomeSpeaker = "ماتيو (Mateo)",
                    outcomeReply = "¡Muchas gracias! Te regalamos un bono para volver gratis con un amigo esta semana.",
                    outcomeArabic = "شكرًا جزيلًا! نهديك قسيمة للعودة مجانًا مع صديق هذا الأسبوع.",
                    feedbackAr = "تعبير '¡Enhorabuena!' (مبروك/تهانينا) أصيل جدًا في إسبانيا وأسعد النادل!"
                ),
                StoryChoice(
                    replyText = "¡Qué detalle tan bonito! ¿Puedo sentarme en la terraza para disfrutar del sol?",
                    arabicHint = "شكر اللفتة وطلب الجلوس في الشرفة تحت الشمس",
                    outcomeSpeaker = "ماتيو (Mateo)",
                    outcomeReply = "¡Claro que sí! Te llevo el pedido a la mesa del rincón, tiene la mejor vista de la plaza.",
                    outcomeArabic = "بالطبع نعم! سأحضر لك الطلب إلى طاولة الزاوية، لها أجمل إطلالة على الساحة.",
                    feedbackAr = "رد لبق وطبيعي استخدمت فيه تعبير '¡Qué detalle!' الشائع جدًا في الثقافة الإسبانية."
                )
            ),
            comprehensionQuestionAr = "لماذا يقدم المقهى مفاجأة اليوم؟",
            comprehensionOptionsAr = listOf("بسبب الطقس الممطر", "بمناسبة ذكرى تأسيس المقهى", "لأنه آخر يوم عمل"),
            correctOptionIndex = 1
        ),
        InteractiveStory(
            id = "story_es_cdmx_detective",
            language = TargetLanguage.ES,
            cefrLevel = CefrLevel.B2,
            genreAr = "تحقيقات وغموض",
            titleAr = "المخطوطة المفقودة في مكسيكو سيتي",
            subtitleTarget = "El manuscrito perdido en Ciudad de México",
            turns = listOf(
                StoryTurn("إيلينا (Elena)", 1.12f, "Inspector, el manuscrito original desapareció de la biblioteca justo antes del cierre.", "أيها المفتش، اختفت المخطوطة الأصلية من المكتبة قبيل الإغلاق مباشرة."),
                StoryTurn("المفتش رويز (Ruiz)", 0.85f, "Curioso... No forzaron la cerradura. Eso significa que alguien usó una llave maestra.", "غريب... لم يكسروا القفل. هذا يعني أن أحدهم استخدم مفتاحًا رئيسيًا."),
                StoryTurn("إيلينا (Elena)", 1.12f, "Solo tres personas tenemos acceso al archivo histórico, pero las cámaras del pasillo estuvieron apagadas diez minutos.", "ثلاثة أشخاص فقط لدينا صلاحية دخول الأرشيف التاريخي، لكن كاميرات الممر توقفت عشر دقائق.")
            ),
            choices = listOf(
                StoryChoice(
                    replyText = "Revisemos primero el registro electrónico de entradas para saber qué tarjeta se escaneó.",
                    arabicHint = "فحص السجل الإلكتروني للبطاقات أولًا بأسلوب تحليلي",
                    outcomeSpeaker = "المفتش رويز (Ruiz)",
                    outcomeReply = "¡Brillante deducción! El registro muestra una entrada a las 19:52 desde el sótano. Acabamos de encontrar la pista clave.",
                    outcomeArabic = "استنتاج عبقري! السجل يظهر دخولًا الساعة 19:52 من القبو. لقد وجدنا الدليل الحاسم للتو.",
                    feedbackAr = "استخدامك لأسلوب الاقتراح 'Revisemos' قاد التحقيق مباشرةً نحو الدليل الإلكتروني!"
                ),
                StoryChoice(
                    replyText = "Entrevistemos al técnico de mantenimiento que revisó las cámaras esta tarde.",
                    arabicHint = "استجواب فني الصيانة الذي فحص الكاميرات عصر اليوم",
                    outcomeSpeaker = "إيلينا (Elena)",
                    outcomeReply = "Es cierto, vino sin cita previa y preguntó dónde guardábamos las ediciones antiguas. ¡Llamemos a seguridad!",
                    outcomeArabic = "صحيح، لقد جاء دون موعد مسبق وسأل أين نحفظ الطبعات القديمة. لنتصل بالأمن!",
                    feedbackAr = "خيار ذكي كشف خيطًا جديدًا ومثيرًا في لغز المخطوطة."
                )
            ),
            comprehensionQuestionAr = "لماذا استنتج المفتش استخدام مفتاح رئيسي؟",
            comprehensionOptionsAr = listOf("لأن النافذة مكسورة", "لأن القفل لم يتعرض للكسر", "لأن الباب بقي مفتوحًا منذ الصباح"),
            correctOptionIndex = 1
        )
    )

    // Verified educational YouTube videos with accurate timestamped dialogue lines for Learn From Real Videos
    val videoLessons: List<VideoLesson> = listOf(
        VideoLesson(
            id = "vid_en_us_cafe",
            youtubeVideoId = "sWct6G95S-E",
            titleAr = "محادثة واقعية في المقهى والمطعم — إنجليزية أمريكية",
            language = TargetLanguage.EN,
            dialect = Dialect.EN_US,
            cefrLevel = CefrLevel.A2,
            lines = listOf(
                TimedSubtitleLine(5, 13, "Hi, welcome in! What can I get started for you today?", "مرحبًا، أهلاً بك! بماذا يمكنني أن أبدأ طلبك اليوم؟", "'Welcome in' تحية أمريكية دافئة جدًا في المقاهي والمتاجر."),
                TimedSubtitleLine(14, 24, "I'd love an iced latte with oat milk, and could you go light on the ice?", "أود لاتيه مثلج بحليب الشوفان، وهل يمكنك تقليل الثلج قليلًا؟", "'go light on...' تعبير أمريكي طبيعي يعني 'قلل من كمية كذا'."),
                TimedSubtitleLine(25, 36, "Sure thing! Would you like anything warmed up from the bakery case?", "بكل سرور! هل تود تسخين أي شيء من قسم المخبوزات؟", "'Sure thing!' رد ودي جدًا يعادل 'بالطبع'."),
                TimedSubtitleLine(37, 48, "Actually, I'll grab an almond croissant to go. That'll be all, thanks!", "في الواقع، سآخذ كرواسون باللوز سفري. هذا كل شيء، شكرًا!", "'That'll be all' طريقة طبيعية لإنهاء الطلب.")
            ),
            quizQuestionAr = "ماذا يعني التعبير الأمريكي (go light on the ice)؟",
            quizOptionsAr = listOf("إضافة الكثير من الثلج", "تقليل كمية الثلج", "تغيير المشروب إلى شاي"),
            correctOptionIndex = 1
        ),
        VideoLesson(
            id = "vid_en_uk_work",
            youtubeVideoId = "jP8i1hE9V8o",
            titleAr = "التواصل المهني والاجتماعات — إنجليزية بريطانية",
            language = TargetLanguage.EN,
            dialect = Dialect.EN_GB,
            cefrLevel = CefrLevel.B2,
            lines = listOf(
                TimedSubtitleLine(4, 14, "Right, shall we kick things off by reviewing last week's milestones?", "حسنًا، هل نبدأ بمراجعة إنجازات الأسبوع الماضي؟", "'kick things off' تعني 'نبدأ الاجتماع أو النشاط' بأسلوب احترافي سلس."),
                TimedSubtitleLine(15, 27, "I'm slightly concerned that the rollout schedule is a bit tight.", "أشعر ببعض القلق من أن جدول الإطلاق ضيق قليلًا.", "البريطانيون يستخدمون التلطيف (slightly / a bit) للتعبير عن قلق مهم بلباقة."),
                TimedSubtitleLine(28, 40, "Fair point. If we streamline the onboarding flow, we can buy ourselves three extra days.", "نقطة وجيهة. إذا بسّطنا خطوات البدء، يمكننا كسب ثلاثة أيام إضافية.", "'buy ourselves time' تعني توفير وقت إضافي."),
                TimedSubtitleLine(41, 52, "Brilliant — let's circle back on Thursday to sign off on the final build.", "رائع — دعنا نعود للموضوع يوم الخميس لاعتماد النسخة النهائية.", "'sign off on' تعني الموافقة النهائية الرسمية.")
            ),
            quizQuestionAr = "ماذا تعني عبارة (sign off on the final build)؟",
            quizOptionsAr = listOf("حذف النسخة", "الاعتماد والموافقة النهائية على النسخة", "إغلاق الحساب"),
            correctOptionIndex = 1
        ),
        VideoLesson(
            id = "vid_es_es_travel",
            youtubeVideoId = "DAp_v7EH9AA",
            titleAr = "جولة يومية وحوارات السفر — إسبانية أوروبية",
            language = TargetLanguage.ES,
            dialect = Dialect.ES_ES,
            cefrLevel = CefrLevel.A2,
            lines = listOf(
                TimedSubtitleLine(3, 12, "¡Hola a todos! Hoy vamos a recorrer el centro histórico paso a paso.", "مرحبًا بالجميع! اليوم سنتجول في المركز التاريخي خطوة بخطوة.", "'recorrer' فعل ممتاز يعني التجول واستكشاف المكان."),
                TimedSubtitleLine(13, 23, "Si llegas temprano, puedes evitar las colas y disfrutar del ambiente tranquilo.", "إذا وصلت مبكرًا، يمكنك تجنب الطوابير والاستمتاع بالأجواء الهادئة.", "'hacer cola / evitar las colas' تعني الانتظار في الطابور أو تجنبه."),
                TimedSubtitleLine(24, 35, "Aquí cerca hay un mercado tradicional donde prueban productos frescos cada mañana.", "هنا بالقرب يوجد سوق تقليدي يتذوقون فيه المنتجات الطازجة كل صباح.", "'Aquí cerca' طريقة مختصرة وطبيعية لقول 'بالقرب من هنا'."),
                TimedSubtitleLine(36, 47, "No te olvides de llevar calzado cómodo porque vamos a caminar bastante.", "لا تنسَ ارتداء حذاء مريح لأننا سنمشي كثيرًا.", "'bastante' تعني 'قدرًا كبيرًا / كثيرًا' وهي شائعة جدًا في إسبانيا.")
            ),
            quizQuestionAr = "ما النصيحة العملية في الجملة الأخيرة؟",
            quizOptionsAr = listOf("ارتداء حذاء مريح للمشي الطويل", "حجز سيارة أجرة", "إحضار مظلة شمسية"),
            correctOptionIndex = 0
        ),
        VideoLesson(
            id = "vid_es_mx_daily",
            youtubeVideoId = "kJQjXAVEwt0",
            titleAr = "تعبيرات الشارع والمحادثة الطبيعية — إسبانية لاتينية",
            language = TargetLanguage.ES,
            dialect = Dialect.ES_MX,
            cefrLevel = CefrLevel.B1,
            lines = listOf(
                TimedSubtitleLine(4, 14, "¿Qué tal? Hoy te voy a compartir frases que usamos todos los días en Latinoamérica.", "كيف الحال؟ اليوم سأشاركك جملًا نستخدمها يوميًا في أمريكا اللاتينية.", "'¿Qué tal?' تحية مرنة تناسب كل اللهجات الإسبانية."),
                TimedSubtitleLine(15, 26, "Cuando algo sale muy bien, decimos con entusiasmo: ¡qué padre o qué chévere!", "عندما ينجح أمر ما بشكل رائع، نقول بحماس: يا للروعة!", "'qué padre' مكسيكية و'qué chévere' واسعة الانتشار في أمريكا اللاتينية."),
                TimedSubtitleLine(27, 38, "Si necesitas pedir un favor con amabilidad, puedes empezar con: ¿me haces un favor?", "إذا احتجت لطلب معروف بلطف، يمكنك البدء بـ: هل تسدي لي معروفًا؟", "'hacer un favor' تعبير يومي لا غنى عنه."),
                TimedSubtitleLine(39, 50, "Escuchar estas expresiones en contexto te ayuda a hablar con mucha más soltura.", "سماع هذه التعبيرات في السياق يساعدك على التحدث بطلاقة أكبر بكثير.", "'con soltura' تعني بسلاسة وطلاقة طبيعية.")
            ),
            quizQuestionAr = "متى يُستخدم تعبير (¡qué chévere!) في الإسبانية اللاتينية؟",
            quizOptionsAr = listOf("عند الغضب", "للتعبير عن الإعجاب والروعة", "عند الاعتذار"),
            correctOptionIndex = 1
        )
    )
}
