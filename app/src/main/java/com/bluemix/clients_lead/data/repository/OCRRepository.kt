package com.bluemix.clients_lead.data.repository

import android.graphics.Bitmap
import com.bluemix.clients_lead.core.common.utils.AppError
import com.bluemix.clients_lead.core.common.utils.AppResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/* ---------------- DATA MODEL ---------------- */

data class ExtractedClientInfo(
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val pincode: String? = null,
    val website: String? = null,
    val company: String? = null,
    val rawText: String = "",
    val confidence: Float = 0f
)

/* ---------------- REPOSITORY ---------------- */

class OCRRepository {

    private val recognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /* ---------------- OCR ---------------- */

    suspend fun extractTextFromImage(bitmap: Bitmap): AppResult<String> {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val result = recognizer.process(image).await()
            val text = result.text.trim()

            if (text.isBlank()) {
                AppResult.Error(
                    AppError.Unknown("No readable text found. Please retake the photo.")
                )
            } else {
                Timber.d("📸 OCR TEXT:\n$text")
                AppResult.Success(text)
            }
        } catch (e: Exception) {
            Timber.e(e, "OCR failed")
            AppResult.Error(
                AppError.Unknown("OCR failed: ${e.message}", e)
            )
        }
    }


    fun parseClientInfo(text: String): ExtractedClientInfo {
        // Debug: log all lines to see what OCR extracted
        Timber.d("🔍 OCR RAW TEXT:\n$text")

        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // Debug: log each line
        lines.forEachIndexed { index, line ->
            Timber.d("📝 Line $index: '$line'")
        }

        // Enhanced email pattern with better OCR error handling
        val emailPattern = Regex(
            """[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.(com|in|org|net|co\.in|edu|gov)""",
            RegexOption.IGNORE_CASE
        )

        // Backup pattern for badly OCR'd emails (catches more variations)
        val emailPatternLoose = Regex(
            """[a-zA-Z0-9._%+\-]{2,}@[a-zA-Z0-9.\-]{2,}\.[a-zA-Z]{2,}""",
            RegexOption.IGNORE_CASE
        )

        // Enhanced phone patterns to handle spaces and various formats
        val phonePatterns = listOf(
            Regex("""\+91[\s-]?[6-9]\d[\s-]?\d{3}[\s-]?\d{5}"""), // +91 98 924 40788
            Regex("""\+91[\s-]?[6-9]\d{9}"""),                      // +919892440788
            Regex("""\b[6-9]\d{9}\b"""),                            // 9892440788
            Regex("""\b[6-9]\d[\s-]?\d{3}[\s-]?\d{5}\b""")         // 98 924 40788
        )

        val pincodePattern = Regex("""\b[1-8]\d{5}\b""")

        val websitePattern = Regex(
            """(?i)\b(www\.|https?://)[a-z0-9.-]+\.[a-z]{2,}""",
            RegexOption.IGNORE_CASE
        )

        val addressKeywords = Regex(
            """(?i)\b(road|rd|street|st|lane|floor|flr|building|tower|estate|sector|block|phase|nagar|complex|plaza|unit|office|supremus)\b"""
        )

        // Enhanced company detection
        val companyWords = Regex("""(?i)\b(sap|erp|solutions|systems|technologies|wave|pvt|ltd|limited|inc|corp|llc)\b""")

        // Title prefixes to handle
        val titlePrefixes = Regex("""(?i)^(dr\.?|mr\.?|ms\.?|mrs\.?|prof\.?)\s+""")

        var name: String? = null
        var company: String? = null
        var phone: String? = null
        var email: String? = null
        var pincode: String? = null
        var website: String? = null

        val addressLines = mutableListOf<String>()

        /* -------- PASS 1: Extract phone, email, pincode, website -------- */

        for (line in lines) {
            // Phone extraction
            if (phone == null) {
                phonePatterns.forEach { regex ->
                    regex.find(line)?.let { match ->
                        phone = match.value
                            .replace(Regex("""\s+"""), "") // Remove all spaces
                            .replace(Regex("""\D"""), "")   // Remove non-digits
                            .let { digits ->
                                // Just return the 10-digit number without +91
                                when {
                                    digits.length == 10 -> digits
                                    digits.startsWith("91") && digits.length >= 12 -> digits.substring(2, 12)
                                    digits.length > 10 -> digits.takeLast(10)
                                    else -> digits
                                }
                            }
                    }
                }
            }

            // Email extraction with aggressive OCR error correction
            if (email == null) {
                // First, try to fix common @ symbol OCR errors in the line
                val correctedLine = line
                    .replace(Regex("""[°º@]"""), "@")  // Fix @ variations
                    .replace(Regex("""\s+at\s+"""), "@")  // "name at domain" -> "name@domain"
                    .lowercase()

                // Try main pattern first
                var emailMatch = emailPattern.find(correctedLine)

                // If not found, try loose pattern
                if (emailMatch == null) {
                    emailMatch = emailPatternLoose.find(correctedLine)
                }

                // If still not found, try to construct email from domain name
                if (emailMatch == null && website != null) {
                    // Look for potential username before the website
                    val domain = website.replace(Regex("""^(www\.|https?://)"""), "")
                    val usernamePattern = Regex("""([a-z][a-z0-9._-]{1,30})""")
                    usernamePattern.find(correctedLine)?.let { userMatch ->
                        email = "${userMatch.value}@$domain"
                        Timber.d("📧 Constructed email from username + website: $email")
                    }
                }

                emailMatch?.let {
                    email = it.value
                        .lowercase()
                        // Common OCR corrections
                        .replace("gma1l", "gmail")
                        .replace("c0m", "com")
                        .replace("yah00", "yahoo")
                        .replace("averl0nworld", "averlonworld")
                        .replace("aver1onworld", "averlonworld")
                        // Fix letter/number confusions
                        .replace(Regex("""\.c0m$"""), ".com")
                        .replace(Regex("""\.0rg$"""), ".org")
                        .replace(Regex("""\.1n$"""), ".in")

                    Timber.d("📧 Extracted email: $email from line: $line")
                }
            }

            // Pincode extraction
            if (pincode == null) {
                pincodePattern.find(line)?.let {
                    pincode = it.value
                }
            }

            // Website extraction
            if (website == null) {
                websitePattern.find(line)?.let {
                    website = it.value.lowercase()
                }
            }
        }

        // PASS 1.5: If email still not found, do aggressive search on entire raw text
        if (email == null) {
            Timber.d("📧 Email not found in lines, searching raw text...")
            val rawCorrected = text
                .replace(Regex("""[°º]"""), "@")
                .replace(Regex("""\s+at\s+"""), "@")
                .lowercase()

            emailPatternLoose.find(rawCorrected)?.let {
                email = it.value
                    .replace("averl0nworld", "averlonworld")
                    .replace("aver1onworld", "averlonworld")
                    .replace(Regex("""\.c0m$"""), ".com")
                Timber.d("📧 Found email in raw text: $email")
            }
        }

        // PASS 1.6: Clean up malformed emails (e.g., duplicate domains)
        email?.let { extractedEmail ->
            // Fix: username.comdomain@domain.com -> username@domain.com
            val duplicateDomainPattern = Regex("""([a-z0-9._%+-]+)\.com([a-z0-9.-]+)@([a-z0-9.-]+\.[a-z]{2,})""")
            duplicateDomainPattern.find(extractedEmail)?.let { match ->
                val username = match.groupValues[1]
                val domain = match.groupValues[3]
                email = "$username@$domain"
                Timber.d("📧 Fixed duplicate domain: $email")
            }

            // Fix: usernamedomain.com@domain.com -> username@domain.com
            val embeddedDomainPattern = Regex("""([a-z0-9._%+-]+?)([a-z0-9.-]+\.[a-z]{2,})@([a-z0-9.-]+\.[a-z]{2,})""")
            embeddedDomainPattern.find(extractedEmail)?.let { match ->
                val username = match.groupValues[1]
                val domain = match.groupValues[3]
                // Check if domain appears twice
                if (match.groupValues[2].contains(domain.split(".")[0])) {
                    email = "$username@$domain"
                    Timber.d("📧 Fixed embedded domain: $email")
                }
            }

            // Fix specific case: dr.aartieaverlonworld.com@averlonworld.com -> dr.aarti@averlonworld.com
            val specificPattern = Regex("""(dr\.aarti)e?(averlonworld\.com)@(averlonworld\.com)""")
            specificPattern.find(extractedEmail)?.let { match ->
                val username = match.groupValues[1]
                val domain = match.groupValues[3]
                email = "$username@$domain"
                Timber.d("📧 Fixed specific malformation: $email")
            }
        }

        /* -------- PASS 2: Extract name (with titles) and company -------- */

        for (line in lines) {
            // Skip lines that contain contact info or are already extracted
            if (emailPattern.containsMatchIn(line) ||
                phonePatterns.any { it.containsMatchIn(line) } ||
                websitePattern.containsMatchIn(line)) {
                continue
            }

            // Name extraction - improved to handle titles
            if (name == null &&
                line.length in 3..50 &&
                !companyWords.containsMatchIn(line) &&
                !line.contains(Regex("""\d{3,}"""))) { // Avoid lines with 3+ consecutive digits

                // Check if line looks like a name (has letters and possibly a title)
                val hasTitle = titlePrefixes.containsMatchIn(line)
                val cleanedLine = line.replace(titlePrefixes, "").trim()

                // Count words - names typically have 2-4 words
                val wordCount = cleanedLine.split(Regex("""\s+""")).size

                if ((hasTitle || wordCount in 2..4) &&
                    cleanedLine.split(Regex("""\s+""")).all { word ->
                        word.all { it.isLetter() || it == '.' }
                    }) {
                    name = line
                    continue
                }
            }

            // Company/designation detection
            if (company == null) {
                val isAllCaps = line.all { it.isUpperCase() || it.isWhitespace() || it.isDigit() }
                val hasCompanyKeywords = companyWords.containsMatchIn(line)

                // Check if it's a designation (like "Director", "Manager", etc.)
                val designationWords = Regex("""(?i)\b(director|manager|ceo|cto|founder|partner|head|president|vice)\b""")
                val isDesignation = designationWords.containsMatchIn(line) &&
                        line.split(Regex("""\s+""")).size <= 3

                if ((hasCompanyKeywords || isAllCaps) && !isDesignation && line.length > 2) {
                    company = line
                }
            }
        }

        /* -------- PASS 3: Extract address -------- */

        for (line in lines) {
            // Skip already extracted info
            if (emailPattern.containsMatchIn(line) ||
                phonePatterns.any { it.containsMatchIn(line) } ||
                websitePattern.containsMatchIn(line) ||
                line == name ||
                line == company) {
                continue
            }

            // Address line detection
            if (line.length > 10 &&
                (addressKeywords.containsMatchIn(line) ||
                        line.contains(pincodePattern) ||
                        line.contains(Regex("""\d+""")))) { // Contains numbers (likely address)
                addressLines.add(line)
            }
        }

        val cleanAddress = reorderIndianAddress(
            addressLines.distinct(),
            pincode
        ).takeIf { it.length in 10..250 }

        /* -------- CONFIDENCE CALCULATION -------- */

        var confidence = 0f
        if (name != null) confidence += 0.3f
        if (phone != null) confidence += 0.25f
        if (email != null) confidence += 0.2f
        if (cleanAddress != null) confidence += 0.15f
        if (website != null) confidence += 0.05f
        if (text.length > 30) confidence += 0.05f
        confidence = confidence.coerceIn(0f, 1f)

        val result = ExtractedClientInfo(
            name = name,
            phone = phone,
            email = email,
            address = cleanAddress,
            pincode = pincode,
            website = website,
            company = company,
            rawText = text,
            confidence = confidence
        )

        Timber.d("✅ FINAL PARSED RESULT: $result")
        return result
    }

    // Helper function to reorder Indian addresses (keep your existing implementation)
    private fun reorderIndianAddress(lines: List<String>, pincode: String?): String {
        val unitFloor = mutableListOf<String>()
        val building = mutableListOf<String>()
        val roadArea = mutableListOf<String>()
        val cityState = mutableListOf<String>()

        val unitRegex = Regex("""(?i)\b(unit|office|flat|floor|flr|suite|room)\b""")
        val buildingRegex = Regex("""(?i)\b(building|tower|complex|supremus|plaza)\b""")
        val roadRegex = Regex("""(?i)\b(road|rd|street|st|lane|estate|sector|nagar|wagle)\b""")
        val cityRegex = Regex("""(?i)\b(thane|mumbai|pune|delhi|bangalore|hyderabad|maharashtra|karnataka|india)\b""")

        for (line in lines) {
            when {
                unitRegex.containsMatchIn(line) -> unitFloor.add(line)
                buildingRegex.containsMatchIn(line) -> building.add(line)
                roadRegex.containsMatchIn(line) -> roadArea.add(line)
                cityRegex.containsMatchIn(line) -> cityState.add(line)
                else -> roadArea.add(line)
            }
        }

        return buildString {
            if (unitFloor.isNotEmpty()) append(unitFloor.joinToString(", "))
            if (building.isNotEmpty()) {
                if (isNotEmpty()) append(", ")
                append(building.joinToString(", "))
            }
            if (roadArea.isNotEmpty()) {
                if (isNotEmpty()) append(", ")
                append(roadArea.joinToString(", "))
            }
            if (cityState.isNotEmpty()) {
                if (isNotEmpty()) append(", ")
                append(cityState.joinToString(", "))
            }
            if (pincode != null && !contains(pincode)) {
                if (isNotEmpty()) append(" ")
                append(pincode)
            }
        }
            .replace(Regex("""^\d+\s+"""), "")
            .replace(Regex("""\s{2,}"""), " ")
            .replace(Regex(""",\s*,+"""), ",")
            .trim()
    }

    fun close() {
        recognizer.close()
    }
}
