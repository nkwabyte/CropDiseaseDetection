package com.nkwabyte.cropdiseasedetection.common.data

data class DiseaseInfo(
    val id: Int,
    val name: String,
    val crop: String, // "Corn", "Pepper", "Tomato"
    val isHealthy: Boolean,
    val description: String,
    val symptoms: String,
    val causes: String,
    val prevention: String,
    val imageUrl: String
)

object DiseaseDatabase {
    val diseases = listOf(
        DiseaseInfo(
            id = 0,
            name = "Corn Cercospora Leaf Spot",
            crop = "Corn",
            isHealthy = false,
            description = "Also known as Gray Leaf Spot, this fungal disease significantly impacts corn yields globally.",
            symptoms = "Small, tan, rectangular lesions running parallel to leaf veins. Over time, these expand into long gray/tan stripes that can coalesce, destroying leaf tissue.",
            causes = "The fungal pathogen Cercospora zeae-maydis, which overwinter in crop residues.",
            prevention = "Plant resistant corn hybrids, practice crop rotation (avoid corn-on-corn), tillage to bury infected residues, and apply timely fungicides if disease pressure is high.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/e/e4/Gray_leaf_spot_corn.jpg"
        ),
        DiseaseInfo(
            id = 1,
            name = "Corn Common Rust",
            crop = "Corn",
            isHealthy = false,
            description = "A fungal disease that spreads rapidly in cool, moist weather, characterized by distinctive powdery brown spots.",
            symptoms = "Cinnamon-brown, powdery pustules appearing on both upper and lower leaf surfaces. The pustules contain spores that turn black late in the season.",
            causes = "Fungal pathogen Puccinia sorghi, whose spores are wind-blown from southern areas.",
            prevention = "Use resistant corn hybrids, destroy host plants or crop residues, and apply fungicides if infection occurs early in the season.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/e/eb/Puccinia_sorghi_on_Zea_mays_01.jpg"
        ),
        DiseaseInfo(
            id = 2,
            name = "Corn Healthy",
            crop = "Corn",
            isHealthy = true,
            description = "Healthy corn plants with vibrant growth, strong structural integrity, and high productivity potential.",
            symptoms = "Vibrant green leaves free from brown spots, streaks, or wilting. Sturdy stalks and uniform ear development.",
            causes = "Optimal nutrients, regular watering, robust crop management, and resistance to local pathogens.",
            prevention = "Continue balanced soil fertility management, monitor crop regularly, practice crop rotation, and maintain consistent irrigation.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/4/4c/Zea_mays_-_healthy_corn_field.jpg"
        ),
        DiseaseInfo(
            id = 3,
            name = "Corn Northern Leaf Blight",
            crop = "Corn",
            isHealthy = false,
            description = "A destructive fungal disease that causes rapid loss of leaf area, reducing the plant's ability to photosynthesize.",
            symptoms = "Large, long, cigar-shaped grayish-green or tan lesions (1 to 6 inches long) that start on lower leaves and move upwards.",
            causes = "Fungal pathogen Exserohilum turcicum, favored by moderate temperatures and high humidity.",
            prevention = "Plant resistant corn hybrids, rotate with non-grass crops, manage residues to reduce spores, and use chemical or organic fungicides.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/6/6d/Northern_corn_leaf_blight.jpg"
        ),
        DiseaseInfo(
            id = 4,
            name = "Corn Streak",
            crop = "Corn",
            isHealthy = false,
            description = "A viral disease prevalent in Africa, causing severe stunting and striped patterns on foliage.",
            symptoms = "Narrow, broken, yellowish-white streaks along leaf veins, creating a distinctive striped pattern. Severe infection leads to stunting and malformed ears.",
            causes = "Maize Streak Virus (MSV), transmitted by leafhopper vectors of the genus Cicadulina.",
            prevention = "Grow MSV-resistant crop varieties, control leafhoppers using insecticides or biological controls, keep fields weed-free, and plant early.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/3/30/Maize_streak_virus.jpg"
        ),
        DiseaseInfo(
            id = 5,
            name = "Pepper Bacterial Spot",
            crop = "Pepper",
            isHealthy = false,
            description = "A bacterial disease that affects leaves, stems, and fruits of peppers, leading to substantial crop loss.",
            symptoms = "Small, dark, water-soaked spots on leaves that turn brown, dry, and scab-like. Severe infections cause leaf yellowing and dropping, leaving fruit vulnerable to sunscald.",
            causes = "Bacterium Xanthomonas campestris pv. vesicatoria, spread by splashing rain and contaminated tools.",
            prevention = "Use pathogen-free seeds and certified disease-free transplants. Apply copper-based bactericides early, rotate crops, and avoid overhead irrigation.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/b/b2/Bacterial_spot_on_pepper_leaf.jpg"
        ),
        DiseaseInfo(
            id = 6,
            name = "Pepper Cercospora",
            crop = "Pepper",
            isHealthy = false,
            description = "Also known as Frogeye Leaf Spot, this fungal infection causes characteristic circular lesions on leaves.",
            symptoms = "Circular lesions with tan-to-gray centers and dark brown margins. Spots may eventually drop out, leaving holes. Leaves may turn yellow and drop.",
            causes = "Fungi Cercospora capsici, promoted by warm, wet, and humid conditions.",
            prevention = "Select clean seeds, rotate crops, remove infected crop residues, maintain plant spacing for good airflow, and apply protective fungicides.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/5/52/Cercospora_capsici_on_pepper.jpg"
        ),
        DiseaseInfo(
            id = 7,
            name = "Pepper Early Blight",
            crop = "Pepper",
            isHealthy = false,
            description = "A fungal infection that targets older foliage first and causes concentric target-like spots.",
            symptoms = "Brown-to-black spots with concentric rings ('target board' appearance) on lower, older leaves. Lesions can lead to defoliation.",
            causes = "Fungal pathogen Alternaria species, thriving in high moisture and warm weather.",
            prevention = "Water at the soil level to keep leaves dry, rotate crops, prune lower leaves, and apply copper-based or systemic fungicides.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/5/5a/Early_blight_on_pepper.jpg"
        ),
        DiseaseInfo(
            id = 8,
            name = "Pepper Fusarium",
            crop = "Pepper",
            isHealthy = false,
            description = "A soil-borne fungal disease that causes vascular wilting and eventual death of pepper plants.",
            symptoms = "Yellowing of lower leaves, followed by progressive wilting that does not recover with watering. Stem base shows dark brown discoloration inside when cut.",
            causes = "The soil pathogen Fusarium oxysporum, which attacks the root system and blocks water transport.",
            prevention = "Plant resistant cultivars, maintain soil pH above 6.5, improve soil drainage, and avoid planting in fields with a history of wilt.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/0/0e/Fusarium_wilt_on_peppers.jpg"
        ),
        DiseaseInfo(
            id = 9,
            name = "Pepper Healthy",
            crop = "Pepper",
            isHealthy = true,
            description = "Healthy pepper plants showing optimal growth, dark green foliage, and healthy fruit setting.",
            symptoms = "Vibrant, green leaves with smooth margins, sturdy upright stems, white blossoms, and firm, glossy peppers free of spots.",
            causes = "Good soil quality, consistent watering, pest control, and proper sun exposure.",
            prevention = "Maintain consistent soil moisture, mulch to retain water and suppress weeds, and apply balanced fertilizers regularly.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/7/7a/Healthy_bell_pepper_plant.jpg"
        ),
        DiseaseInfo(
            id = 10,
            name = "Pepper Late Blight",
            crop = "Pepper",
            isHealthy = false,
            description = "A highly destructive disease caused by water mold that affects leaves, stems, and fruits.",
            symptoms = "Dark, water-soaked lesions on leaves and stems. Under high humidity, a white, cottony growth can be observed on the undersides of leaves.",
            causes = "Phytophthora infestans (oomycete), which spreads rapidly in cool, wet environments.",
            prevention = "Remove and destroy infected plants immediately, improve air circulation, avoid wet foliage, and use targeted copper fungicides.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/d/df/Late_blight_on_pepper_stem.jpg"
        ),
        DiseaseInfo(
            id = 11,
            name = "Pepper Leaf Blight",
            crop = "Pepper",
            isHealthy = false,
            description = "A general term for fungal or bacterial pathogens causing rapid leaf necrosis and defoliation.",
            symptoms = "Large, irregular dark brown or water-soaked patches on leaves, starting at the margins or tips. Rapid leaf death and defoliation.",
            causes = "Pathogens like Phytophthora capsici under extremely wet and warm conditions.",
            prevention = "Rotate crops, plant on raised beds to improve drainage, avoid handling wet plants, and use preventive fungicides.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/9/98/Phytophthora_capsici_on_bell_pepper.jpg"
        ),
        DiseaseInfo(
            id = 12,
            name = "Pepper Leaf Curl",
            crop = "Pepper",
            isHealthy = false,
            description = "A viral disease causing severe leaf distortion, leaf puckering, and stunting.",
            symptoms = "Leaves curl dramatically upwards or downwards, puckering, wrinkling, and showing yellowing. Plants are stunted and produce little to no fruit.",
            causes = "Viruses like Chilli Leaf Curl Virus, which are transmitted by whiteflies or thrips.",
            prevention = "Control whiteflies and thrips with insecticidal soap, yellow sticky traps, or neem oil. Remove infected host plants.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/f/fe/Chilli_leaf_curl_disease.jpg"
        ),
        DiseaseInfo(
            id = 13,
            name = "Pepper Leaf Mosaic",
            crop = "Pepper",
            isHealthy = false,
            description = "A viral disease causing mottling of foliage and malformed fruits.",
            symptoms = "Alternating light green, yellow, and dark green patches (mosaic pattern) on leaves. Leaves may be narrowed or distorted. Stunted plant growth.",
            causes = "Viruses such as Tobacco Mosaic Virus (TMV) or Cucumber Mosaic Virus (CMV), spread by aphids or mechanical contact.",
            prevention = "Grow mosaic-resistant pepper varieties, control aphids, sanitize garden tools, and wash hands before handling plants.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/0/07/Tobacco_mosaic_virus_on_pepper.jpg"
        ),
        DiseaseInfo(
            id = 14,
            name = "Pepper Septoria",
            crop = "Pepper",
            isHealthy = false,
            description = "A fungal disease that creates numerous small spots on pepper leaves, leading to defoliation.",
            symptoms = "Numerous small, circular spots with dark brown margins and greyish centers. Tiny black dots (pycnidia) can be seen inside the spots.",
            causes = "Fungal pathogen Septoria species, favored by warm temperatures and rain splash.",
            prevention = "Practice crop rotation, remove old plant debris, avoid overhead irrigation, and use organic copper fungicides.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/d/d4/Septoria_on_pepper_leaf.jpg"
        ),
        DiseaseInfo(
            id = 15,
            name = "Tomato Bacterial Spot",
            crop = "Tomato",
            isHealthy = false,
            description = "A destructive bacterial disease affecting foliage, stems, and fruit under warm, wet conditions.",
            symptoms = "Small, circular-to-irregular dark brown spots on leaves, stems, and green fruits. Lesions on fruit are raised, scab-like, and dark.",
            causes = "Bacterium Xanthomonas species, spread by splashing rain, dew, and agricultural machinery.",
            prevention = "Plant certified disease-free seeds and transplants. Apply copper sprays early in wet seasons, rotate crops, and prune to improve airflow.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/e/ea/Bacterial_spot_on_tomato_leaves.jpg"
        ),
        DiseaseInfo(
            id = 16,
            name = "Tomato Early Blight",
            crop = "Tomato",
            isHealthy = false,
            description = "One of the most common fungal diseases in tomatoes, starting at the bottom of the plant.",
            symptoms = "Dark spots with concentric rings (resembling targets) appearing on older leaves first. Foliage yellows and dies from the bottom up. Dark, sunken spots on stems.",
            causes = "Fungal pathogen Alternaria solani, which overwinters in soil and plant debris.",
            prevention = "Prune lower leaves to prevent soil splash, apply a thick layer of mulch, space plants well, rotate crops, and apply preventive fungicides.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/5/5d/Early_blight_of_tomato.jpg"
        ),
        DiseaseInfo(
            id = 17,
            name = "Tomato Fusarium",
            crop = "Tomato",
            isHealthy = false,
            description = "A soil-borne fungal wilt that blocks the plant's vascular system, leading to wilting and death.",
            symptoms = "Yellowing of leaves, often starting on one side of the branch or plant. Wilting of foliage during warm hours. Stems show brown vascular discoloration inside.",
            causes = "Soil-borne fungus Fusarium oxysporum f. sp. lycopersici, which survives indefinitely in the soil.",
            prevention = "Always plant resistant tomato varieties (marked with F, FF, or FFF). Keep soil pH neutral (around 6.5-7.0) and avoid over-fertilizing with nitrogen.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/e/ef/Fusarium_wilt_on_tomato.jpg"
        ),
        DiseaseInfo(
            id = 18,
            name = "Tomato Healthy",
            crop = "Tomato",
            isHealthy = true,
            description = "Healthy tomato plants exhibiting vibrant growth and high yields of clean fruit.",
            symptoms = "Uniform, deep-green compound leaves, strong stems, yellow blossoms, and smooth, round green or red fruits without spots.",
            causes = "Proper soil pH, regular deep watering, adequate spacing, and balanced fertilizer application.",
            prevention = "Support plants with stakes or cages, mulch to conserve moisture, provide consistent watering, and prune non-productive suckers.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/d/de/Healthy_tomato_plant_with_green_tomatoes.jpg"
        ),
        DiseaseInfo(
            id = 19,
            name = "Tomato Late Blight",
            crop = "Tomato",
            isHealthy = false,
            description = "A highly contagious disease capable of destroying entire tomato fields within days during cool, humid weather.",
            symptoms = "Large, dark, greasy-looking gray-brown spots on leaves and stems. Undersides of leaves may show a white, downy fungal growth. Fruit develops large, firm, brown lesions.",
            causes = "Water mold Phytophthora infestans, spread by wind-borne spores.",
            prevention = "Plant resistant cultivars. Keep leaves dry by watering at the base, space plants for excellent air circulation, and apply copper fungicides immediately if blight is reported.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/c/c9/Tomato_late_blight.jpg"
        ),
        DiseaseInfo(
            id = 20,
            name = "Tomato Leaf Curl",
            crop = "Tomato",
            isHealthy = false,
            description = "Also known as Tomato Yellow Leaf Curl, this virus causes severe stunting and leaf deformation.",
            symptoms = "Leaves curl dramatically upwards and inwards, with yellowed margins. Leaves become small, thick, and leathery. Severe stunting of plant growth and failure to set fruit.",
            causes = "Tomato Yellow Leaf Curl Virus (TYLCV), transmitted solely by whiteflies.",
            prevention = "Use protective fine-mesh netting to exclude whiteflies from young seedlings. Apply neem oil or insecticidal soap, and plant resistant varieties.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/0/0b/Tomato_yellow_leaf_curl_virus.jpg"
        ),
        DiseaseInfo(
            id = 21,
            name = "Tomato Mosaic",
            crop = "Tomato",
            isHealthy = false,
            description = "A highly stable virus that causes mottling and blistering of leaves and reduces fruit quality.",
            symptoms = "Mottled light-green and dark-green patches on leaves. Leaf blades may be distorted, thin, or stringy ('fern leaf'). Sunken brown streaks can appear on stems.",
            causes = "Tobacco Mosaic Virus (TMV) or Tomato Mosaic Virus (ToMV), highly infectious and spread by touch or tools.",
            prevention = "Grow virus-resistant cultivars. Wash hands and sterilize tools with a bleach solution or soap before working, and discard infected plants.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/9/9c/Tomato_mosaic_virus_leaves.jpg"
        ),
        DiseaseInfo(
            id = 22,
            name = "Tomato Septoria",
            crop = "Tomato",
            isHealthy = false,
            description = "A common fungal disease causing extensive spotting and defoliation of lower tomato leaves.",
            symptoms = "Numerous small, circular spots with dark brown borders and grey centers. Center of spots shows tiny black dots (pycnidia). Leaves yellow and fall off.",
            causes = "Fungus Septoria lycopersici, spread by water splashes onto lower leaves.",
            prevention = "Mulch around the base of plants, prune the lower branches, avoid overhead watering, rotate crops, and apply copper-based fungicides.",
            imageUrl = "https://upload.wikimedia.org/wikipedia/commons/6/6f/Septoria_lycopersici_on_tomato.jpg"
        )
    )
}
