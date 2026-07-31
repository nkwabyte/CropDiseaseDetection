package com.nkwabyte.cropdiseasedetection.common.data

data class DiseaseInfo(
    val id: Int,
    val name: String,
    val localName: String,          // Descriptive local Akan/Twi name used in Ghana
    val crop: String,               // "Corn", "Pepper", "Tomato"
    val isHealthy: Boolean,
    val description: String,        // Short summary paragraph
    val symptoms: String,
    val causes: String,
    val effects: String,            // Impact on plant / yield / farmer livelihood
    val prevention: String,         // General cultural / agronomic prevention
    val organicMitigation: String,  // Organic / biological control measures
    val chemicalMitigation: String, // Chemical / synthetic control measures
    val imageUrl: String
)

object DiseaseDatabase {
    val diseases = listOf(

        // ── CORN ─────────────────────────────────────────────────────────────────

        DiseaseInfo(
            id = 0,
            name = "Corn Cercospora Leaf Spot",
            localName = "Aburo Nhwiren Tuntum (Corn Gray Leaf Disease)",
            crop = "Corn",
            isHealthy = false,
            description = "Also known as Gray Leaf Spot, this fungal disease is one of the most economically damaging corn diseases globally. It thrives in warm, humid conditions and can cause severe yield losses when infection occurs early in the season.",
            symptoms = "Small, tan to gray, rectangular lesions running parallel to leaf veins. Over time, lesions expand into long gray or tan stripes that coalesce, destroying large areas of leaf tissue. Heavily infected fields appear scorched and gray.",
            causes = "The fungal pathogen Cercospora zeae-maydis, which overwinters in corn crop residues on the soil surface. Spores are released during warm, humid weather and spread by wind and splashing rain.",
            effects = "Reduces photosynthetic leaf area, causing yield losses of 10–50%. Severe infections weaken stalks, making them prone to lodging (falling over) before harvest. Grain quality is also reduced in badly infected fields.",
            prevention = "Use certified disease-resistant hybrid seeds. Practice crop rotation — avoid planting corn after corn. Bury infected residues through tillage. Maintain adequate plant spacing to improve air circulation. Scout fields regularly from mid-season.",
            organicMitigation = "• Apply Trichoderma-based biofungicides (Trichoderma harzianum) to soil and as foliar spray.\n• Use neem oil (Azadirachta indica) extract spray at 3–4 week intervals.\n• Apply compost tea as a foliar spray to boost plant immunity.\n• Remove and destroy heavily infected leaves and debris after harvest.\n• Intercrop with legumes to break disease cycle and improve soil health.",
            chemicalMitigation = "• Azoxystrobin (Quadris): Apply at VT (tasseling) to R1 (silking) stage. Repeat after 14 days if needed.\n• Pyraclostrobin + Boscalid (Pristine): Highly effective; apply preventively.\n• Propiconazole (Tilt): Apply at first sign of disease; follow label dosage.\n• Trifloxystrobin: Can be tank-mixed with propiconazole for broader control.\n• Always rotate fungicide modes of action to prevent resistance.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/e/e4/Gray_leaf_spot_corn.jpg"
        ),

        DiseaseInfo(
            id = 1,
            name = "Corn Common Rust",
            localName = "Aburo Nhwiren Atoro (Corn Rust/Red Spot Disease)",
            crop = "Corn",
            isHealthy = false,
            description = "A fungal rust disease that spreads rapidly in cool, moist weather. Characteristic powdery brick-red pustules appear on both leaf surfaces. Widespread in West Africa, including Ghana, particularly during the cool early-morning humid conditions.",
            symptoms = "Cinnamon-brown to brick-red, powdery pustules (uredia) scattered on both upper and lower leaf surfaces. Later in the season pustules turn black (telia). Severely infected leaves may yellow and die prematurely.",
            causes = "Fungal pathogen Puccinia sorghi. Spores are wind-blown from southern regions. Infection favored by cool temperatures (16–23°C) and extended dew periods (6+ hours leaf wetness).",
            effects = "Reduces photosynthesis, causing yield losses of 5–40% depending on infection timing. Early infection before tasseling is most damaging. Severely infected plants also have reduced kernel weight and poor grain fill.",
            prevention = "Plant rust-resistant hybrid varieties — the most effective long-term solution. Early planting reduces exposure to peak rust spore periods. Destroy volunteer corn and alternative host plants. Monitor fields weekly from the V8 growth stage onward.",
            organicMitigation = "• Plant naturally resistant or tolerant corn varieties (check with local seed suppliers).\n• Destroy infected crop residues and volunteer corn after harvest.\n• Neem oil spray (2–3% solution) at first sign of infection.\n• Sulfur dust or wettable sulfur can suppress early infections.\n• Ensure adequate potassium in soil — deficient plants are more susceptible.",
            chemicalMitigation = "• Mancozeb (Dithane M-45): Protective fungicide; apply at first pustule appearance.\n• Propiconazole (Tilt 250 EC): Systemic; apply before VT stage for best results.\n• Tebuconazole (Folicur): Effective systemic; apply when disease incidence reaches 5%.\n• Azoxystrobin: Provides both protective and curative activity.\n• Apply fungicide before silking if rust is present — post-silking applications are less cost-effective.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/e/eb/Puccinia_sorghi_on_Zea_mays_01.jpg"
        ),

        DiseaseInfo(
            id = 2,
            name = "Corn Healthy",
            localName = "Aburo Pa (Good / Healthy Corn)",
            crop = "Corn",
            isHealthy = true,
            description = "This corn plant shows no signs of disease, pest damage, or nutritional stress. Healthy corn exhibits vibrant green foliage, strong structural integrity, and is on track for optimal grain yield.",
            symptoms = "Vibrant uniform green leaves free from brown spots, streaks, chlorosis, or wilting. Sturdy upright stalks, well-developed tassel, and uniform ear development with good silk coverage.",
            causes = "Optimal soil fertility, consistent and appropriate irrigation or rainfall, use of certified quality seeds, proper plant population, and good integrated crop management practices.",
            effects = "High grain yields, good kernel weight, strong stalks resistant to lodging. The plant is capable of achieving full genetic yield potential with reduced post-harvest losses.",
            prevention = "Continue current management practices. Perform regular field scouting (at least weekly) to catch any early disease or pest signs before they escalate. Maintain records of applied inputs for informed future decisions.",
            organicMitigation = "• Maintain regular compost or organic matter applications to sustain soil biology.\n• Practice crop rotation with legumes (cowpea, soybean) to improve soil nitrogen naturally.\n• Use intercropping to maximize land use and suppress weeds.\n• Continue balanced foliar micronutrient applications if used.\n• Install water harvesting structures (ridges, mulch) to conserve moisture.",
            chemicalMitigation = "• Apply balanced NPK fertilizer (e.g., 15-15-15 or 23-10-5) at recommended rates.\n• Side-dress with urea (46-0-0) at knee-high stage (V6) for nitrogen boost.\n• Apply preventive fungicide program at tasseling if previous seasons had disease pressure.\n• Use insecticides for stemborers only if damage threshold (10% infestation) is reached.\n• Follow CABI or MoFA (Ghana Ministry of Food and Agriculture) spray calendar recommendations.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/4/4c/Zea_mays_-_healthy_corn_field.jpg"
        ),

        DiseaseInfo(
            id = 3,
            name = "Corn Northern Leaf Blight",
            localName = "Aburo Nhwiren Ohow (Corn Leaf Burning Disease)",
            crop = "Corn",
            isHealthy = false,
            description = "A destructive fungal disease causing rapid loss of leaf area, significantly reducing photosynthesis. Large cigar-shaped tan lesions are the hallmark symptom. Particularly severe in areas with moderate temperatures and extended dew periods, common in Ghana's forest transition zones.",
            symptoms = "Large, long, cigar-shaped gray-green to tan lesions (3–15 cm long) that run with the leaf veins. Lesions typically start on lower leaves and progress upward. Infected tissue turns tan/gray and lesions may develop a dark sooty appearance from fungal sporulation.",
            causes = "Fungal pathogen Exserohilum turcicum (formerly Helminthosporium turcicum). Survives on corn residues. Favored by moderate temperatures (18–27°C) and extended leaf wetness (>6 hours). Spores spread by wind and rain splash.",
            effects = "Can cause 30–50% yield loss when infection occurs before or at tasseling. Significant defoliation reduces ear fill and kernel weight. Severe early infections can result in complete crop failure in susceptible varieties.",
            prevention = "Plant resistant or tolerant hybrid varieties — most important single practice. Rotate crops with non-grass species (e.g., legumes, vegetables). Plow under infected residues after harvest to reduce inoculum. Avoid excessive nitrogen fertilization which promotes lush, susceptible tissue.",
            organicMitigation = "• Use resistant corn varieties (seek CSIR-SARI or IITA recommended varieties in Ghana).\n• Apply Trichoderma harzianum as soil amendment and foliar spray.\n• Neem cake application to soil reduces soil inoculum.\n• Remove and destroy infected plant material after harvest — do not leave residues.\n• Intercrop with cowpea to break the disease cycle.",
            chemicalMitigation = "• Propiconazole (Tilt 250 EC): Apply at V8–V10 stage before tasseling; repeat at 14-day intervals.\n• Azoxystrobin: Highly effective preventive; apply at first disease sign.\n• Pyraclostrobin + Metconazole (Headline AMP): Broad-spectrum; excellent for NLB control.\n• Mancozeb: Protective fungicide; apply as preventive during disease-favorable weather.\n• Fungicide applications are most economical when applied before silking.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/6/6d/Northern_corn_leaf_blight.jpg"
        ),

        DiseaseInfo(
            id = 4,
            name = "Corn Streak",
            localName = "Aburo Nhwiren Mfirimiri (Corn Striped Leaf Disease)",
            crop = "Corn",
            isHealthy = false,
            description = "Maize Streak Disease is Africa's most economically important corn virus disease. Caused by Maize Streak Virus (MSV) and transmitted by leafhoppers, it is endemic throughout sub-Saharan Africa including Ghana. Highly susceptible varieties can suffer near-total crop failure.",
            symptoms = "Narrow, broken, yellowish-white streaks develop along leaf veins, creating a distinctive pale green-to-yellow striped pattern. Severely infected plants are stunted with reduced or malformed ears. Leaves may become bleached with entire yellowing in advanced stages.",
            causes = "Maize Streak Virus (MSV), a Geminivirus transmitted by leafhoppers (Cicadulina spp.). The virus cannot spread through soil or direct contact — it requires the leafhopper vector. Leafhoppers are most abundant in hot, dry weather.",
            effects = "30–100% yield loss depending on the susceptibility of the variety and the age of infection. Early infection (within 2 weeks of germination) causes near-total crop failure. Stunted plants produce no viable grain. A major constraint to smallholder farmers in Ghana.",
            prevention = "The most effective strategy is planting MSV-resistant varieties — varieties with the MSV resistance gene (e.g., STRIGA 6, STRIGA 4, or IITA-released streaky-resistant varieties). Early planting reduces exposure to peak leafhopper populations. Remove weeds from field margins which serve as leafhopper reservoirs.",
            organicMitigation = "• Plant certified MSV-resistant/tolerant varieties (strongly recommended).\n• Early planting (begin of rains): plants establish before peak leafhopper season.\n• Intercrop corn with cowpea or cassava to disrupt leafhopper movement.\n• Rogue out (remove) severely infected plants early to reduce virus reservoir.\n• Keep field edges free of grass weeds which harbor leafhoppers.\n• Sticky yellow traps can help monitor leafhopper populations.",
            chemicalMitigation = "• Imidacloprid seed treatment (Gaucho 600 FS): Very effective at protecting young seedlings from leafhopper feeding during the critical 3-week establishment period.\n• Thiamethoxam seed treatment (Cruiser): Alternative to Imidacloprid for early protection.\n• Foliar insecticides (Dimethoate, Chlorpyrifos): Apply to control leafhoppers if seed treatment was not used; apply at first leafhopper sighting.\n• Note: Chemical control kills the vector but cannot cure infected plants. Focus on prevention.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/3/30/Maize_streak_virus.jpg"
        ),

        // ── PEPPER ───────────────────────────────────────────────────────────────

        DiseaseInfo(
            id = 5,
            name = "Pepper Bacterial Spot",
            localName = "Mako Nhwiren Ponko (Pepper Spotted Leaf Disease)",
            crop = "Pepper",
            isHealthy = false,
            description = "A destructive bacterial disease affecting leaves, stems, and fruits of peppers under warm, wet conditions. Widely present in Ghana's humid agricultural zones. Early defoliation and fruit scarring cause substantial economic losses, particularly for export-quality peppers.",
            symptoms = "Small, dark water-soaked circular spots on leaves (2–10 mm) that turn brown with yellow halos. Spots may merge, causing large necrotic patches. Leaf yellowing and dropping (defoliation) exposes fruit to sunscald. Fruit develops raised, dark, scab-like lesions reducing marketability.",
            causes = "Bacterium Xanthomonas campestris pv. vesicatoria (also X. perforans and X. euvesicatoria). Spreads by rain splash, overhead irrigation, contaminated tools, infected seeds, and transplants. Infection favored by warm temperatures (25–30°C) and leaf wetness.",
            effects = "Severe defoliation weakens plants and exposes developing fruits to sunscald and secondary infections. Fruit lesions (scabs) reduce market value by 40–60%. Yield losses of up to 50% in wet seasons. Export-quality peppers are rejected due to fruit spotting.",
            prevention = "Use only certified disease-free seeds and transplants from reputable sources. Avoid overhead irrigation — use drip or furrow irrigation. Minimize movement through wet fields. Disinfect pruning tools between plants. Practice 2–3 year crop rotation with non-solanaceous crops.",
            organicMitigation = "• Use copper-based organic sprays (Bordeaux mixture: 1% copper sulfate + lime) as preventive treatment every 7–10 days.\n• Apply neem oil (2%) + castile soap (0.5%) spray to reduce bacterial load and boost plant defenses.\n• Remove and destroy infected leaves and plant debris.\n• Mulch around plant base to prevent soil splash onto lower leaves.\n• Compost applications improve soil biology and plant resilience.",
            chemicalMitigation = "• Copper hydroxide (Kocide 3000): Most commonly used; apply every 7 days in wet weather.\n• Copper oxychloride + Mancozeb (Cuprosan): Effective combination for bacterial and fungal control.\n• Streptomycin sulfate (where legally permitted): Apply at early infection stage, rotate with copper.\n• Acibenzolar-S-methyl (Actigard): Plant activator that induces systemic resistance — use with copper.\n• Do not apply copper in hot, sunny conditions to avoid phytotoxicity.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/b/b2/Bacterial_spot_on_pepper_leaf.jpg"
        ),

        DiseaseInfo(
            id = 6,
            name = "Pepper Cercospora",
            localName = "Mako Nhwiren Akyekyere (Pepper Frog-Eye Leaf Disease)",
            crop = "Pepper",
            isHealthy = false,
            description = "Also known as Frogeye Leaf Spot, this fungal infection causes characteristic circular lesions with pale centers and dark margins that resemble frog eyes. Common in Ghana during humid, rainy periods. Repeated infections across the growing season cause progressive defoliation.",
            symptoms = "Circular lesions (5–15 mm) with tan-to-gray centers and distinct dark brown to purple margins. Centers may drop out leaving shot-holes. Leaves may yellow around lesions and eventually drop. Heavy infection causes widespread defoliation.",
            causes = "Fungus Cercospora capsici. Survives in infected plant debris. Spores produced and spread during warm (25–30°C), humid, wet conditions. Also transmitted through infected seeds.",
            effects = "Progressive defoliation reduces plant photosynthesis and exposes fruit to sunscald. Yield losses of 20–40% are common in unmanaged fields. Repeated defoliation weakens plants making them susceptible to other pathogens.",
            prevention = "Plant from certified clean seeds. Maintain adequate plant spacing (60 cm between plants) for good air circulation. Remove infected plant material promptly. Practice crop rotation with non-solanaceous crops for at least 2 years.",
            organicMitigation = "• Copper sulfate solution (Bordeaux mixture) spray applied preventively every 10–14 days.\n• Neem oil + bicarbonate solution spray (1 tsp baking soda + 1 tbsp neem oil per liter).\n• Remove and destroy all infected leaves and debris.\n• Apply Trichoderma harzianum as soil drench to reduce soilborne inoculum.\n• Good spacing and pruning to improve airflow reduces humidity around plants.",
            chemicalMitigation = "• Mancozeb (Dithane M-45): Protective; apply every 7–10 days in wet weather.\n• Chlorothalonil (Bravo 500): Very effective protectant; apply every 7–14 days.\n• Azoxystrobin (Amistar): Systemic; apply at first sign of disease.\n• Difenoconazole (Score 250 EC): Systemic; apply every 14 days for curative effect.\n• Rotate fungicides between chemical classes every 2–3 applications to prevent resistance.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/5/52/Cercospora_capsici_on_pepper.jpg"
        ),

        DiseaseInfo(
            id = 7,
            name = "Pepper Early Blight",
            localName = "Mako Nhwiren Ohow (Pepper Leaf Burn Disease)",
            crop = "Pepper",
            isHealthy = false,
            description = "A fungal infection targeting older, lower foliage first, creating distinctive concentric ring spots resembling a target or bullseye. Common throughout Ghana's pepper-growing regions, particularly during the rainy season when temperatures are warm and moisture persists.",
            symptoms = "Brown-to-black spots (10–15 mm) with concentric rings giving a 'target board' appearance on lower, older leaves. Yellow halos may surround spots. Lesions can cause leaves to yellow and fall off. In severe cases, defoliation progresses rapidly upward.",
            causes = "Fungal pathogens Alternaria capsici or A. alternata. Soilborne and seedborne. Survives on infected debris. Favored by warm temperatures (24–29°C), high humidity, and alternate wet and dry cycles.",
            effects = "Defoliation begins at the base of the plant and moves upward, reducing photosynthesis significantly. Exposed fruits develop sunscald and secondary rot. Yield losses can reach 30–50% in heavily infected fields without management.",
            prevention = "Water plants at the soil level only — avoid wetting foliage. Rotate crops with non-solanaceous plants for 2+ years. Stake plants to keep foliage off the ground. Remove lower leaves in contact with soil. Avoid excessive nitrogen fertilization which promotes lush, susceptible growth.",
            organicMitigation = "• Apply compost mulch (5 cm thick) around plant base to prevent soil splash onto leaves.\n• Copper sulfate (Bordeaux mixture) spray as preventive treatment.\n• Neem oil spray (2–3%) to suppress fungal growth.\n• Bicarbonate + neem oil foliar spray changes leaf surface pH to inhibit fungi.\n• Remove and destroy infected lower leaves as soon as spotted.",
            chemicalMitigation = "• Chlorothalonil (Bravo 500): Apply every 7–10 days as preventive during wet/humid seasons.\n• Iprodione (Rovral): Curative fungicide for Alternaria; apply at early infection.\n• Azoxystrobin (Amistar): Systemic; apply preventively at planting and every 14 days.\n• Boscalid + Pyraclostrobin (Bellis): Premium combination product effective against Alternaria.\n• Copper oxychloride: Apply preventively every 10 days during the rainy season.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/5/5a/Early_blight_on_pepper.jpg"
        ),

        DiseaseInfo(
            id = 8,
            name = "Pepper Fusarium",
            localName = "Mako Kobuo (Pepper Wilt Disease)",
            crop = "Pepper",
            isHealthy = false,
            description = "A devastating soil-borne fungal disease that invades the vascular system of pepper plants, causing progressive wilting and eventual plant death. Once established in a field, the pathogen persists in the soil for many years. Common in irrigated pepper production systems in Ghana.",
            symptoms = "Yellowing of lower leaves progressing upward. Wilting of shoots during hot daytime hours that does not recover with watering. Plants eventually collapse and die. Internal stem discoloration — brown-to-reddish-brown vascular streaking visible when stem is cut near the base.",
            causes = "Soil-borne fungus Fusarium oxysporum f.sp. capsici. Enters roots through wounds or natural openings. Survives as chlamydospores in soil for 20+ years. Favored by warm soil temperatures (25–30°C), high soil moisture after drought stress, and low soil pH.",
            effects = "Plant death within days to weeks of infection. Total crop loss is possible when entire field becomes infected. Soil remains contaminated for years — future pepper crops at same site will suffer. No recovery is possible once a plant is infected.",
            prevention = "Do not replant peppers in fields with a history of Fusarium wilt for at least 5 years. Maintain soil pH above 6.5 with lime applications. Improve soil drainage — avoid waterlogging. Sterilize transplanting tools. Source transplants only from certified, clean nurseries.",
            organicMitigation = "• Soil solarization: Cover moist soil with clear plastic for 4–6 weeks during hot season to reduce soilborne inoculum significantly.\n• Trichoderma harzianum soil amendment (drench or mix into transplant holes): colonizes roots and suppresses Fusarium.\n• Mycorrhizal fungi inoculants (Glomus spp.) applied at transplanting to enhance root health.\n• Apply neem cake (ground neem seed) to soil — has fungicidal properties against Fusarium.\n• Use resistant or tolerant varieties where available.",
            chemicalMitigation = "• Metalaxyl (Ridomil): Soil drench at transplanting as preventive; does not cure infected plants.\n• Carbendazim (Bavistin): Soil drench or seed treatment; use preventively only.\n• Thiram seed treatment: Protects seedling roots during early establishment.\n• Fludioxonil: Seed treatment fungicide effective against Fusarium crown rot.\n• Important: No chemical provides a cure once infection is established — prevention is essential.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/0/0e/Fusarium_wilt_on_peppers.jpg"
        ),

        DiseaseInfo(
            id = 9,
            name = "Pepper Healthy",
            localName = "Mako Pa (Good / Healthy Pepper)",
            crop = "Pepper",
            isHealthy = true,
            description = "This pepper plant exhibits optimal health with dark green foliage, strong stems, and good fruit development. Healthy pepper plants indicate proper agronomic practices are in place and the crop is on course for productive yields.",
            symptoms = "Vibrant, dark green leaves with smooth margins and no spots or yellowing. Sturdy upright stems. Abundant white or purple blossoms. Firm, glossy peppers that are properly sized and free of blemishes.",
            causes = "Good soil quality (pH 6.0–6.8), consistent moisture management, adequate plant spacing, balanced nutrition (NPK + micronutrients), and effective pest and weed control.",
            effects = "High fruit yield with good market-quality peppers. Strong plants are more resilient to drought and disease pressure. Consistent production supports farmer income and household food security.",
            prevention = "Maintain consistent soil moisture through drip irrigation or regular watering. Apply mulch to conserve moisture and suppress weeds. Monitor regularly for early signs of pests and diseases. Keep field records to track inputs and yields.",
            organicMitigation = "• Apply well-decomposed compost (2–3 kg per plant) at planting and as top-dressing.\n• Mulch with straw or dry grass (8–10 cm) to conserve moisture and suppress weeds.\n• Companion plant with basil or marigolds to repel aphids and whiteflies naturally.\n• Use neem oil spray preventively every 2–3 weeks to deter pests and minor fungal issues.\n• Foliar spray with fish emulsion or diluted fermented plant juice to boost plant vigor.",
            chemicalMitigation = "• Apply balanced NPK fertilizer (e.g., 12-12-17 for fruiting stage).\n• Supplement with calcium nitrate to prevent blossom end rot.\n• Apply magnesium sulfate (Epsom salt foliar spray) if yellowing between veins is seen.\n• Preventive copper spray at the start of the rainy season.\n• Use systemic insecticide seed treatment at nursery stage to protect young seedlings.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/7/7a/Healthy_bell_pepper_plant.jpg"
        ),

        DiseaseInfo(
            id = 10,
            name = "Pepper Late Blight",
            localName = "Mako Nhwiren Kwanee (Pepper Leaf Die-Off Disease)",
            crop = "Pepper",
            isHealthy = false,
            description = "A highly destructive disease caused by the water mold Phytophthora infestans (same pathogen as the Irish Potato Famine). It can destroy entire pepper fields within days during cool, wet weather. Very rapid spread makes it one of the most feared crop diseases globally.",
            symptoms = "Dark, greasy-looking, water-soaked lesions on leaves and stems that spread rapidly. White cottony mold growth on undersides of leaves in humid conditions. Stems develop dark brown lesions and may collapse. Fruit develops firm, dark brown lesions.",
            causes = "Phytophthora infestans (oomycete, not a true fungus). Spreads via wind-blown sporangia during cool (15–20°C), wet conditions. Can travel several kilometers by wind. Thrives on cool nights with morning dew.",
            effects = "Entire field destruction possible within 3–7 days under favorable conditions. Fruit infection renders the crop unsalable. Significant economic losses especially during late-season rainy periods in Ghana.",
            prevention = "Avoid dense planting — maintain good air circulation. Avoid overhead irrigation. Monitor forecasts during cool rainy periods and apply fungicides preventively. Remove and burn all infected plant material immediately. Do not leave infected crop debris in the field.",
            organicMitigation = "• Bordeaux mixture (copper sulfate 1% + lime): Apply every 5–7 days during cool, wet weather as strong preventive.\n• Remove and destroy all infected plants and leaves immediately — bag and burn; do not compost.\n• Improve field drainage to reduce standing water.\n• Avoid planting in low-lying areas prone to cool air pooling.\n• Use copper-based soap spray (copper octanoate) as an organic-approved alternative.",
            chemicalMitigation = "• Metalaxyl + Mancozeb (Ridomil Gold MZ): Most effective; apply preventively every 7–10 days during cool, wet weather.\n• Cymoxanil + Mancozeb (Curzate M): Effective with both protective and curative activity; apply every 5–7 days.\n• Dimethomorph (Forum): Systemic oomycicide; effective against Phytophthora; rotate with Ridomil.\n• Fosetyl-aluminum (Aliette): Systemic; absorbed by roots and leaves; apply preventively.\n• Rotate fungicide modes of action every 2–3 applications to prevent resistance.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/d/df/Late_blight_on_pepper_stem.jpg"
        ),

        DiseaseInfo(
            id = 11,
            name = "Pepper Leaf Blight",
            localName = "Mako Nhwiren Opapaee (Pepper Leaf Rot Disease)",
            crop = "Pepper",
            isHealthy = false,
            description = "A general term for rapid necrosis and defoliation caused by fungal or bacterial pathogens affecting pepper leaves. Characterized by large, irregular dark lesions that spread quickly, leading to significant leaf death and weakening of the plant.",
            symptoms = "Large, irregular dark brown or water-soaked patches on leaves starting at the margins or leaf tips. Rapid necrosis (cell death) causing patches to turn brown and papery. Severe infections lead to complete leaf death and defoliation, leaving the plant bare.",
            causes = "Primarily Phytophthora capsici under extremely warm (28–32°C), wet conditions. Can also be caused by Botrytis cinerea (gray mold) in humid, cool conditions, or by Colletotrichum capsici (anthracnose) in high-humidity environments.",
            effects = "Rapid defoliation exposes fruit to sunscald and secondary pathogens. Premature fruit drop reduces yield significantly. Repeated cycles of defoliation severely weaken plants and can lead to permanent plant decline.",
            prevention = "Raise planting beds (raised bed cultivation) to improve drainage. Avoid overhead irrigation. Reduce planting density to improve air circulation. Avoid handling plants when wet. Remove and destroy infected material promptly.",
            organicMitigation = "• Bordeaux mixture spray as preventive treatment every 10–14 days.\n• Apply potassium silicate foliar spray to strengthen leaf cell walls against infection.\n• Remove infected leaves and plant debris immediately.\n• Improve soil drainage with raised beds and organic matter incorporation.\n• Apply Bacillus subtilis biofungicide (e.g., Serenade) as foliar spray.",
            chemicalMitigation = "• Metalaxyl + Mancozeb: For Phytophthora-caused blight; apply preventively.\n• Copper hydroxide + Mancozeb: Broad-spectrum; apply at disease onset.\n• Iprodione (Rovral 500SC): Effective against Botrytis blight; apply during cool humid conditions.\n• Azoxystrobin: Systemic; broad-spectrum activity against most leaf blight pathogens.\n• Apply a tank mix of contact and systemic fungicide for faster action.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/9/98/Phytophthora_capsici_on_bell_pepper.jpg"
        ),

        DiseaseInfo(
            id = 12,
            name = "Pepper Leaf Curl",
            localName = "Mako Nhwiren Kurukuruwa (Pepper Curled Leaf Disease)",
            crop = "Pepper",
            isHealthy = false,
            description = "A viral disease causing severe leaf curling, puckering, and stunting. Transmitted exclusively by whiteflies, it spreads rapidly through pepper fields especially during the dry season when whitefly populations peak. There is no cure; prevention and vector control are essential.",
            symptoms = "Leaves curl dramatically upwards or downwards and pucker or wrinkle. Curled leaves are often reduced in size and have yellowish margins. Infected plants become stunted, fail to flower properly, and produce little or no fruit. Whole plant appears distorted.",
            causes = "Chilli Leaf Curl Virus (ChiLCV) or Tomato Leaf Curl Virus (TLCV) transmitted by the silverleaf whitefly (Bemisia tabaci). The virus cannot spread through soil, tools, or direct plant contact — it requires the whitefly vector.",
            effects = "Infected plants are unproductive — complete crop failure if plants are infected during early vegetative stage. The virus spreads rapidly through the field via whitefly migration. Total crop loss is possible in severe outbreaks, especially during the dry season.",
            prevention = "Establish nurseries under insect-proof netting to produce virus-free transplants. Maintain a whitefly-free environment during the first 3–4 weeks after transplanting. Remove and destroy infected plants early to reduce virus reservoir. Plant pepper away from tomato, tobacco, or other solanaceous crops.",
            organicMitigation = "• Use reflective silver/aluminum mulch: Deters whiteflies by confusing them with reflected UV light.\n• Install yellow sticky traps (30 per hectare): Monitors and catches whitefly adults.\n• Neem oil spray (3–4%) + insecticidal soap (0.5%) weekly: Kills whitefly nymphs and eggs.\n• Companion plant with marigolds (Tagetes spp.) around field perimeter: Repels whiteflies.\n• Remove and destroy all infected plants immediately to reduce virus reservoir.",
            chemicalMitigation = "• Imidacloprid (Confidor 200SL): Soil drench or foliar spray; highly effective against whiteflies; apply at first sighting.\n• Thiamethoxam (Actara 25WG): Systemic; soil drench at transplanting protects plants for 4–6 weeks.\n• Spiromesifen (Oberon): Effective against whitefly nymphs and eggs; rotate with neonicotinoids.\n• Pyriproxyfen (Admiral): Insect growth regulator; disrupts whitefly development.\n• Note: No chemical can cure viral infection once it occurs — focus all efforts on vector control.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/f/fe/Chilli_leaf_curl_disease.jpg"
        ),

        DiseaseInfo(
            id = 13,
            name = "Pepper Leaf Mosaic",
            localName = "Mako Nhwiren Mfirimiri (Pepper Mosaic / Striped Leaf Disease)",
            crop = "Pepper",
            isHealthy = false,
            description = "A viral disease causing mottled mosaic patterns of light and dark green or yellow on leaves, often accompanied by leaf distortion and stunted growth. Spread by aphids and through mechanical contact, making it highly contagious in managed crop settings.",
            symptoms = "Alternating light green, yellow, and dark green patches (mosaic pattern) on leaf surfaces. Leaves may become narrow, wrinkled, or distorted. Young growing tips may show stunting and distortion. Plants produce smaller, often distorted fruits with reduced quality.",
            causes = "Tobacco Mosaic Virus (TMV), Cucumber Mosaic Virus (CMV), or Pepper Mosaic Virus (PepMV). TMV spreads by mechanical contact (hands, tools) and through infected seed. CMV is spread primarily by aphids (Myzus persicae, Aphis gossypii).",
            effects = "Yield reductions of 30–70% in infected plants. Poor fruit set, distorted small fruits with reduced market value. No recovery is possible for infected plants. Aphid-spread viruses can rapidly infect entire fields if aphid populations are high.",
            prevention = "Grow mosaic-resistant pepper varieties (available from IITA and CSIR-SARI Ghana). Control aphid vectors — the primary spreaders. Sanitize all tools with 10% bleach solution or 70% alcohol before and after working in the field. Wash hands before handling plants. Avoid smoking near plants (TMV source).",
            organicMitigation = "• Neem oil spray (2%) weekly for aphid control — effective against both adults and nymphs.\n• Reflective silver mulch: Deters aphids from landing.\n• Remove infected plants immediately — bag, remove from field, and destroy.\n• Spray diluted skimmed milk (1 part milk : 9 parts water) on tools and hands — denatures TMV.\n• Companion plant with coriander, dill, or fennel to attract aphid predators (ladybirds, lacewings).",
            chemicalMitigation = "• Imidacloprid (Confidor) or Acetamiprid (Mospilan): Systemic insecticide for aphid control; apply at first aphid sighting.\n• Malathion (Malathion 57 EC): Contact insecticide effective against aphids; use as knockdown spray.\n• Mineral oil sprays (horticultural oil): Reduces aphid transmission efficiency of CMV.\n• Pymetrozine (Chess WG): Selective aphicide that does not harm beneficials; rotates well with neonicotinoids.\n• No chemical cures viral infections — all measures must focus on preventing infection.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/0/07/Tobacco_mosaic_virus_on_pepper.jpg"
        ),

        DiseaseInfo(
            id = 14,
            name = "Pepper Septoria",
            localName = "Mako Nhwiren Ponko Ketewa (Pepper Small Spotted Leaf Disease)",
            crop = "Pepper",
            isHealthy = false,
            description = "A fungal disease that creates numerous small, distinct spots on pepper leaves, leading to progressive defoliation. The pathogen thrives during warm, wet periods. While not as dramatic as blight, repeated infections over the season can significantly reduce plant productivity.",
            symptoms = "Numerous small, circular spots (2–5 mm) with dark brown margins and gray-white centers. Tiny black dots (pycnidia — fungal fruiting bodies) visible within the centers with a hand lens. Affected leaves turn yellow and drop prematurely.",
            causes = "Fungal pathogen Septoria capsici. Survives in infected plant debris and soil. Spores spread by rain splash and overhead irrigation. Warm temperatures (20–25°C) with high humidity and moisture on leaves favor infection.",
            effects = "Progressive defoliation from the base of the plant upward, reducing photosynthesis and fruit yield. Yield losses of 25–50% can occur in unmanaged fields during wet seasons. Repeated defoliation cycles weaken plants and reduce their productive lifespan.",
            prevention = "Apply thick organic mulch around plant base to prevent soil splash onto lower leaves. Avoid overhead irrigation — use drip irrigation. Maintain 60 cm+ spacing for good air circulation. Remove and destroy infected plant debris after harvest. Practice 2–3 year crop rotation.",
            organicMitigation = "• Mulch heavily (straw, rice husk) around plants to prevent soil-splash dispersal of spores.\n• Copper-based spray (Bordeaux mixture): Apply every 10–14 days as preventive.\n• Remove infected lower leaves as soon as spotted to slow disease spread.\n• Neem oil + bicarbonate foliar spray to suppress fungal growth.\n• Ensure good plant spacing and prune lower branches to improve airflow.",
            chemicalMitigation = "• Mancozeb (Dithane M-45): Protective; apply every 7–10 days during wet periods.\n• Chlorothalonil (Bravo 500): Effective contact fungicide; apply every 10–14 days.\n• Tebuconazole (Folicur 250EW): Systemic; curative and preventive; apply at disease onset.\n• Azoxystrobin: Systemic; highly effective; apply every 14 days.\n• Rotate fungicide classes every 2–3 applications to manage resistance risk.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/d/d4/Septoria_on_pepper_leaf.jpg"
        ),

        // ── TOMATO ───────────────────────────────────────────────────────────────

        DiseaseInfo(
            id = 15,
            name = "Tomato Bacterial Spot",
            localName = "Ntomato Nhwiren Ponko (Tomato Spotted Leaf Disease)",
            crop = "Tomato",
            isHealthy = false,
            description = "A destructive bacterial disease affecting foliage, stems, and fruit under warm, wet conditions. Common in Ghana's tomato-growing regions during the rainy season. Fruit lesions significantly reduce market value and shelf life, causing major economic losses.",
            symptoms = "Small, circular-to-irregular dark brown spots (2–5 mm) on leaves with yellow halos. Spots may merge into larger necrotic areas. Stems develop raised, dark, elongated lesions. Green fruits develop raised, dark brown, scab-like spots that persist to the ripe fruit stage.",
            causes = "Bacterium Xanthomonas perforans (race T3/T4), X. vesicatoria, or related species. Spread by rain splash, dew, overhead irrigation, contaminated tools, and infected seeds or transplants. Infection requires free moisture on leaf surfaces for at least 30 minutes.",
            effects = "Severe defoliation exposes fruit to sunscald. Fruit lesions (scabs) reduce fresh market value by 50–80%. Yield losses of up to 50% in epidemic years. Export-quality tomatoes are rejected due to blemishes. Post-harvest shelf life is reduced on infected fruit.",
            prevention = "Use certified disease-free seeds and transplants exclusively. Avoid overhead irrigation — use drip irrigation. Practice 3-year crop rotation with non-solanaceous crops. Minimize work in the field when plants are wet. Sanitize tools between rows with 10% bleach solution.",
            organicMitigation = "• Bordeaux mixture (1% copper sulfate + 0.5% lime) spray every 7–10 days during wet periods.\n• Neem oil + castile soap spray as additional preventive between copper sprays.\n• Mulch heavily around plant bases to prevent soil splash (source of inoculum).\n• Remove and destroy infected leaves and plant debris regularly.\n• Compost applications improve soil microbial diversity and plant immune responses.",
            chemicalMitigation = "• Copper hydroxide (Kocide 3000 or Funguran OH): Apply every 7–10 days; standard treatment.\n• Copper oxychloride + Mancozeb (Cuprosan or Curzate): Effective combination for bacterial + fungal control.\n• Streptomycin sulfate (Agri-Mycin): Use alternately with copper; limited to 2–3 applications to prevent resistance.\n• Kasugamycin (Kasumin): Alternative antibiotic bactericide effective against Xanthomonas.\n• Avoid copper applications in hot sunny weather to prevent phytotoxicity (leaf burning).",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/e/ea/Bacterial_spot_on_tomato_leaves.jpg"
        ),

        DiseaseInfo(
            id = 16,
            name = "Tomato Early Blight",
            localName = "Ntomato Nhwiren Ohow Ntem (Tomato Early Leaf Burning)",
            crop = "Tomato",
            isHealthy = false,
            description = "One of the most prevalent fungal diseases in tomatoes globally, and extremely common in Ghana. The classic 'target board' spot pattern on lower/older leaves is the signature symptom. Severe infections can cause complete defoliation, reducing yield by 50–80%.",
            symptoms = "Dark spots (5–15 mm) with concentric rings resembling a bullseye pattern on older lower leaves first. Yellow halo may surround spots. As infection spreads upward, leaves yellow and drop prematurely. Stems may develop dark, sunken elongated lesions (collar rot in seedlings).",
            causes = "Fungal pathogen Alternaria solani (and A. alternata). Soilborne and seedborne pathogen. Survives in crop debris. Favored by warm temperatures (24–29°C), high humidity, and alternating wet and dry cycles. Rain splash and wind spread spores.",
            effects = "Defoliation progresses from the bottom of the plant upward, significantly reducing photosynthesis. Severe defoliation exposes fruits to sunscald and reduces sugar production. Yield losses of 50–80% are possible in severe seasons. Weakened plants are vulnerable to secondary infections.",
            prevention = "Prune lower leaves that touch the soil to prevent splash-up inoculation. Apply thick mulch (grass or straw) around plant base. Space plants at least 60 cm apart. Stake or cage plants to keep foliage off the ground. Practice 3-year rotation with non-solanaceous crops.",
            organicMitigation = "• Mulch with straw or dry grass (8–10 cm deep) to prevent soil splash — the primary infection route.\n• Prune infected lower leaves immediately and destroy them.\n• Copper sulfate (Bordeaux mixture) spray every 10 days as preventive.\n• Neem oil (2%) foliar spray for minor early infections.\n• Calcium foliar spray (calcium nitrate or calcium chloride at 0.5%) strengthens cell walls, slowing infection spread.",
            chemicalMitigation = "• Chlorothalonil (Bravo 500 or Daconil): Most commonly used; apply every 7–10 days preventively.\n• Mancozeb (Dithane M-45): Effective protective fungicide; apply in rotation with Chlorothalonil.\n• Azoxystrobin (Amistar): Systemic; apply every 14 days; excellent for integrated programs.\n• Difenoconazole (Score 250EC): DMI fungicide; apply when disease is first observed.\n• Boscalid + Pyraclostrobin (Signum): Premium SDHI + strobilurin combination for season-long control.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/5/5d/Early_blight_of_tomato.jpg"
        ),

        DiseaseInfo(
            id = 17,
            name = "Tomato Fusarium",
            localName = "Ntomato Kobuo (Tomato Wilt Disease)",
            crop = "Tomato",
            isHealthy = false,
            description = "A devastating soil-borne fungal wilt that blocks the plant's vascular system, preventing water and nutrient transport. Once soil is contaminated, the pathogen persists indefinitely. A major constraint to tomato production in Ghana's forest and transition zones where soil temperatures are favorable.",
            symptoms = "Yellowing of leaves, typically beginning on one side of a branch or plant. Wilting of foliage during warm parts of the day. Wilting does not improve with irrigation. Brown-to-reddish-brown discoloration of vascular tissue visible when stem is cut near the base.",
            causes = "Soil-borne fungus Fusarium oxysporum f. sp. lycopersici (races 1, 2, and 3). Survives as chlamydospores in soil indefinitely (20+ years). Warm soil (25–30°C) and acid soils (pH below 5.5) favor pathogen activity. Enters through roots via wounds or natural openings.",
            effects = "Plant death within days to weeks after symptom appearance. Total crop loss possible in highly infested fields. Soil contamination persists for decades — future crops at same site at risk. No plant recovery is possible once infected. Major driver of land abandonment by tomato farmers.",
            prevention = "Plant only Fusarium-resistant varieties (look for F, FF, or FFF on seed packets — indicates resistance to race 1, 2, and 3). Maintain soil pH at 6.5–7.0 by liming. Improve soil drainage. Source transplants only from certified, disease-free nurseries. Never replant tomatoes in infected soil for at least 5–7 years.",
            organicMitigation = "• Soil solarization: Wet soil thoroughly, then cover with clear 50-micron plastic for 4–6 weeks in the hot season — reduces soilborne Fusarium by 90% in the top 15 cm.\n• Trichoderma harzianum (e.g., Trichomax, Biomax-T): Mix into transplant holes or apply as soil drench; competes with and parasitizes Fusarium.\n• Mycorrhizal inoculant (Glomus intraradices): Apply to roots at transplanting; enhances nutrient uptake and root health.\n• Neem cake: Incorporate 200 g per plant into transplant hole.\n• Compost amendment: Improves soil microbial diversity which suppresses Fusarium.",
            chemicalMitigation = "• Carbendazim (Bavistin 50WP): Soil drench at transplanting as a preventive measure only.\n• Metalaxyl + Thiram: Seed treatment to protect seedling roots during germination.\n• Fludioxonil (Maxim): Seed treatment highly effective against Fusarium crown and root rot.\n• Metam sodium (soil fumigant): Used in severe cases for field fumigation before planting — requires professional application.\n• Critical note: No systemic fungicide can cure Fusarium wilt once vascular tissue is colonized.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/e/ef/Fusarium_wilt_on_tomato.jpg"
        ),

        DiseaseInfo(
            id = 18,
            name = "Tomato Healthy",
            localName = "Ntomato Pa (Good / Healthy Tomato)",
            crop = "Tomato",
            isHealthy = true,
            description = "This tomato plant exhibits vibrant health with strong growth and high productive potential. Healthy tomatoes with proper management produce abundant yields of high-quality fruit, supporting farmer profitability and food security across Ghana.",
            symptoms = "Uniform deep-green compound leaves without spots, streaks, yellowing, or wilting. Strong sturdy stems supporting the plant's weight. Yellow star-shaped blossoms. Smooth, uniformly-colored green fruits transitioning normally to red when ripe.",
            causes = "Proper soil pH (6.0–6.8), regular and even watering (1–1.5 inches per week), balanced NPK fertilization with micronutrients, adequate plant spacing, and consistent integrated pest and disease management.",
            effects = "High fruit yield, good fruit size, excellent taste, color, and shelf life. Strong plants are resistant to common stress factors. Consistent productivity enables farmer profitability and household food security.",
            prevention = "Continue current good management. Scout plants twice weekly for early disease or pest indicators. Maintain support stakes or tomato cages as plants grow. Prune non-productive suckers to direct energy to fruit. Keep records of plantings, inputs, and yields.",
            organicMitigation = "• Apply well-composted organic matter (3–5 kg per plant) at planting.\n• Mulch with straw or grass (10 cm thick) to conserve moisture and suppress weeds.\n• Support plants with bamboo stakes or wire cages to prevent stem damage and improve airflow.\n• Companion plant with basil (repels thrips and aphids) and marigolds (repels nematodes and whiteflies).\n• Fermented plant juice (FPJ) or compost tea as foliar spray to boost plant immunity.",
            chemicalMitigation = "• Apply NPK 15-15-15 at planting, then switch to low-N fertilizer (e.g., 8-24-24) at fruiting stage.\n• Calcium nitrate foliar spray (0.5%) at flowering to prevent blossom end rot.\n• Preventive copper spray (Bordeaux mixture) at the start of the rainy season.\n• Preventive insecticide program for whiteflies and aphids (use Imidacloprid seed treatment at nursery stage).\n• Apply potassium sulfate (SOP) at fruit fill stage to improve fruit quality and disease resistance.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/d/de/Healthy_tomato_plant_with_green_tomatoes.jpg"
        ),

        DiseaseInfo(
            id = 19,
            name = "Tomato Late Blight",
            localName = "Ntomato Nhwiren Kwanee (Tomato Leaf Die-Off Disease)",
            crop = "Tomato",
            isHealthy = false,
            description = "The most feared tomato disease worldwide — the same pathogen caused the 1840s Irish Potato Famine. It can destroy entire tomato fields in 7–10 days during cool, wet weather. Devastates tomato production during Ghana's rainy seasons. Rapid action is critical upon first detection.",
            symptoms = "Large (2–3 cm), dark, greasy-looking gray-brown lesions on leaves and stems. Under high humidity, white downy (fuzzy) sporangiophore growth on undersides of leaves. Firm, dark brown, greasy lesions on fruit. Infected stems collapse. Entire fields can appear scorched within days.",
            causes = "Phytophthora infestans (water mold/oomycete). Wind-dispersed sporangia can travel over 50 km. Cool temperatures (10–20°C) at night and high humidity are optimal for infection. A single infected transplant or infected potato field nearby can initiate an epidemic.",
            effects = "Total crop destruction within 7–10 days under ideal disease conditions. Fruit infection makes the entire crop unsalable. Massive economic losses — a single late blight epidemic can devastate an entire farming community. One of the most economically damaging plant diseases in the world.",
            prevention = "Plant resistant tomato varieties (e.g., Ferline, Legend, Defiant) if available. Monitor weather forecasts — cool nights + high humidity = high risk. Apply fungicides preventively before the first symptoms appear during high-risk weather. Keep field dry — avoid overhead irrigation. Scout fields daily during cool rainy periods.",
            organicMitigation = "• Bordeaux mixture (3–4% copper sulfate + 3% lime): Spray every 5–7 days during cool wet weather — this is the key organic preventive treatment.\n• Immediately remove and destroy ALL infected plant material — bag, remove from field, do not compost.\n• Improve field drainage and avoid planting in low-lying areas.\n• Copper octanoate (Cueva): Organic-approved copper fungicide; apply every 5 days during high-risk periods.\n• Avoid overhead irrigation during cool periods — use drip irrigation only.",
            chemicalMitigation = "• Metalaxyl + Mancozeb (Ridomil Gold MZ 68WG): Premier treatment; apply every 7 days preventively during cool wet periods — do not wait for symptoms.\n• Cymoxanil + Famoxadone (Equation Pro): Systemic + protectant combination; apply every 5–7 days.\n• Dimethomorph (Forum): Systemic oomycicide; effective after early infection; rotate with Ridomil.\n• Fluopicolide + Propamocarb (Infinito): Excellent systemic activity; apply every 7–10 days.\n• Rotate all fungicide groups every 2 applications to prevent the highly resistance-prone Phytophthora from developing tolerance.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/c/c9/Tomato_late_blight.jpg"
        ),

        DiseaseInfo(
            id = 20,
            name = "Tomato Leaf Curl",
            localName = "Ntomato Nhwiren Kurukuruwa (Tomato Curled Leaf Disease)",
            crop = "Tomato",
            isHealthy = false,
            description = "Also known as Tomato Yellow Leaf Curl (TYLCV), this whitefly-transmitted virus is one of the most destructive tomato diseases globally. Transmitted exclusively by the silverleaf whitefly Bemisia tabaci. Devastating in Ghana's dry season when whitefly populations are at their peak.",
            symptoms = "Leaves curl upwards and inwards dramatically with yellowed (chlorotic) margins. Infected leaves are smaller, thick, and leathery. Internode shortening causes a bushy, compact appearance. Severe stunting — plants barely grow beyond 30 cm. Plants rarely set fruit; any fruits that form are small and off-color.",
            causes = "Tomato Yellow Leaf Curl Virus (TYLCV), a Begomovirus transmitted only by Bemisia tabaci (silverleaf whitefly). A single viruliferous whitefly can infect a plant within minutes of feeding. No other route of transmission exists — no soil or tool spread.",
            effects = "Complete crop failure if plants are infected during the first 3–4 weeks of transplanting. Plants infected later in the season may yield 20–30% of expected production. The virus spreads rapidly — a single whitefly colony can infect an entire field within 2 weeks. Major economic losses especially in dry season tomato production.",
            prevention = "Establish nurseries under insect-proof fine mesh (50 mesh) to produce certified whitefly-free transplants. Establish new plots far from existing infected tomato fields. Remove and destroy all volunteer tomato plants which serve as virus reservoirs. Monitor whitefly populations using yellow sticky traps from transplanting.",
            organicMitigation = "• Fine-mesh insect netting (50 mesh) tunnels over seedling beds: prevents whitefly access to young plants during the critical establishment phase.\n• Reflective silver/aluminum mulch: Deters whiteflies; highly effective in early season.\n• Yellow sticky traps (20–30 per hectare): Traps and monitors adult whiteflies.\n• Neem oil spray (3–4%) + insecticidal soap weekly: Controls whitefly nymphs and eggs.\n• Remove and destroy all infected plants immediately — they serve as a permanent virus source for the entire field.",
            chemicalMitigation = "• Imidacloprid (Confidor 200SL) soil drench at transplanting: Systemic protection for 4–6 weeks — the most effective early protection method.\n• Thiamethoxam (Actara 25WG) seed treatment or soil drench: Excellent systemic control of whiteflies.\n• Spiromesifen (Oberon): Targets whitefly eggs and nymphs; rotate with neonicotinoids to prevent resistance.\n• Buprofezin (Applaud): Insect growth regulator; disrupts whitefly molting; use in rotation.\n• Critical: No antiviral treatment exists — all chemical efforts must target the whitefly vector before virus transmission occurs.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/0/0b/Tomato_yellow_leaf_curl_virus.jpg"
        ),

        DiseaseInfo(
            id = 21,
            name = "Tomato Mosaic",
            localName = "Ntomato Nhwiren Mfirimiri (Tomato Mosaic / Striped Leaf Disease)",
            crop = "Tomato",
            isHealthy = false,
            description = "A highly stable contact-transmitted virus causing mottling and blistering of tomato leaves. Tobacco Mosaic Virus (TMV) is one of the most studied plant pathogens — it is extraordinarily stable and can remain infectious in dried tobacco for decades. Infected tools and hands are the primary transmission routes in managed crops.",
            symptoms = "Alternating patches of light green and dark green on leaves (mosaic mottling). Leaf blades may be distorted, narrow, or stringy ('fern leaf' or 'shoestring' symptom). Leaf surfaces may feel bumpy (blistering). Sunken brown or dark streaks on stems. Fruit may show yellow mottling or internal browning.",
            causes = "Tobacco Mosaic Virus (TMV) or Tomato Mosaic Virus (ToMV). Spreads primarily by mechanical contact: hands, tools, clothing, sucking insects. TMV is extraordinarily stable — survives in dried tobacco products, soil, and on surfaces for months to years. Infected seeds can transmit the virus.",
            effects = "Yield reductions of 10–35% through reduced fruit set and smaller, lower-quality fruits. Mottled fruits are unmarketable. TMV is extremely persistent — fields can remain infested for years. No infected plant recovers. Rapid spread in high-density planting systems.",
            prevention = "Plant only virus-resistant varieties (Tm-2 resistance gene — found in most modern commercial tomato varieties). Sterilize all tools with 10% bleach solution or 70% ethyl alcohol before use and between plants. Wash hands thoroughly with soap before handling plants. Never smoke or handle tobacco products near tomato plants (TMV source). Remove infected plants promptly.",
            organicMitigation = "• Plant TMV/ToMV-resistant varieties (Tm-2 or Tm-2² gene) — single most effective protection.\n• Skim milk spray (1:10 dilution with water): Denatures viral proteins on plant surfaces and tools; spray on tools and hands before working.\n• Remove and destroy infected plants immediately — do not compost; bag and burn.\n• Hot water seed treatment (50°C for 30 minutes) to eliminate seed-borne virus before planting.\n• Avoid excessive nitrogen fertilization which increases plant succulence and viral susceptibility.",
            chemicalMitigation = "• No chemical can cure viral infections in plants.\n• Imidacloprid or Acetamiprid: Control sucking insects (aphids, thrips) that may mechanically inoculate plants while feeding.\n• Plant activators (Acibenzolar-S-methyl / Actigard): Stimulates plant systemic acquired resistance; may reduce symptom severity in some cases.\n• Mineral oil (horticultural oil): Applied to leaf surfaces may reduce mechanical transmission efficiency.\n• All management efforts must focus on hygiene, resistant varieties, and infected plant removal.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/9/9c/Tomato_mosaic_virus_leaves.jpg"
        ),

        DiseaseInfo(
            id = 22,
            name = "Tomato Septoria",
            localName = "Ntomato Nhwiren Ponko Ketewa (Tomato Small Spotted Leaf Disease)",
            crop = "Tomato",
            isHealthy = false,
            description = "One of the most common tomato fungal diseases, causing extensive spotting and progressive defoliation from the base of the plant upward. Very widespread in Ghana's humid rainy season. While rarely killing plants outright, repeated defoliation severely reduces yield and fruit quality.",
            symptoms = "Numerous small (2–5 mm), circular spots with distinct dark brown borders and white-to-gray centers on leaves. Tiny black dots (pycnidia — fungal spore cases) visible in the center of spots with magnification. Affected leaves turn yellow and drop. Disease progresses from lower leaves upward.",
            causes = "Fungus Septoria lycopersici. Soilborne pathogen that survives in infected debris and soil. Spread by rain splash, overhead irrigation, and contact with infected plants. Favored by warm temperatures (20–25°C) and extended periods of leaf wetness.",
            effects = "Progressive defoliation from the base upward reduces photosynthesis and exposes fruits to sunscald. Yield losses of 30–60% are common in unmanaged fields during humid seasons. Repeated defoliation weakens plants and reduces the season's total productive output. Quality of remaining fruit is compromised by sun exposure.",
            prevention = "Apply thick organic mulch (5–8 cm) around plant base to prevent rain splash of soil (primary spore source) onto lower leaves. Prune lower leaves once plants are established. Use drip irrigation — avoid wetting foliage. Maintain 60 cm+ plant spacing for good airflow. Practice 3-year crop rotation with non-solanaceous crops.",
            organicMitigation = "• Mulch heavily around plants (straw, rice husk, dry grass): Prevents soil splash — the main source of infection.\n• Prune infected lower leaves promptly and remove from field.\n• Copper sulfate (Bordeaux mixture) spray every 10–14 days as preventive during wet periods.\n• Bicarbonate + neem oil spray (1 tbsp neem oil + 1 tsp baking soda per liter water): Changes leaf surface pH to inhibit fungal spore germination.\n• Compost tea foliar application: Beneficial microbes compete with and suppress Septoria spores.",
            chemicalMitigation = "• Chlorothalonil (Bravo 500 or Daconil 2787): Most commonly used; apply every 7–10 days preventively.\n• Mancozeb (Dithane M-45): Effective protective fungicide; apply in rotation with Chlorothalonil.\n• Azoxystrobin (Amistar or Quadris): Highly effective systemic; apply every 14 days.\n• Propiconazole (Tilt 250EC): DMI systemic fungicide; apply at early disease stage for curative effect.\n• Trifloxystrobin + Tebuconazole (Nativo 75WG): Premium combination; excellent season-long Septoria control.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/6/6f/Septoria_lycopersici_on_tomato.jpg"
        )
    )

    fun getDiseaseInfo(diseaseName: String): DiseaseInfo {
        return diseases.firstOrNull { it.name.equals(diseaseName, ignoreCase = true) }
            ?: diseases.firstOrNull { it.name.contains(diseaseName, ignoreCase = true) }
            ?: diseases.first()
    }
}
