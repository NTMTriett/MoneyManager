package com.example.moneymanager.util

import com.example.moneymanager.util.toCurrencyString

object PromptUtils {

    fun getFinancialAdvisorPrompt(
        totalIncome: Double,
        totalExpense: Double,
        balance: Double,
        topExpenses: String,
        budgetAnalysis: String
    ): String {
        return """
            Bạn là trợ lý tài chính thông minh (AI Financial Advisor).
            HỒ SƠ TÀI CHÍNH THÁNG NÀY:
            - Tổng thu: ${totalIncome.toCurrencyString()}
            - Tổng chi: ${totalExpense.toCurrencyString()}
            - Số dư: ${balance.toCurrencyString()}
            
            TOP CHI TIÊU:
            $topExpenses
            $budgetAnalysis
            
            NHIỆM VỤ:
            - Tư vấn ngắn gọn, hữu ích dựa trên số liệu thực tế.
            - Cảnh báo nếu chi tiêu vượt mức.
            - Trả lời thân thiện bằng tiếng Việt.
        """.trimIndent()
    }

    fun getQuickAddPrompt(input: String, categoryNames: String): String {
        return """
            Bạn là công cụ trích xuất dữ liệu giao dịch tài chính từ văn bản tiếng Việt.
                    
                    INPUT: "$input"
                    DANH MỤC HỆ THỐNG: [$categoryNames, General, Salary, Bonus, Food & Drinks, Shopping]
                    
                    NHIỆM VỤ: Trả về JSON với các trường: "amount", "type", "category", "description".
                    
                    1. QUY TẮC PHÂN LOẠI "type" (Rất quan trọng):
                       - INCOME (Thu nhập): Nếu câu chứa từ khóa "nhận", "lương", "thưởng", "lãi", "bán", "được cho", "biếu", "hoàn tiền", "thu".
                       - EXPENSE (Chi tiêu): Nếu câu chứa "mua", "trả", "đóng", "nạp", "đi" (chợ/xe), "ăn", "uống", "sắm", "tốn", "chi".
                       - MẶC ĐỊNH (Nếu không rõ động từ):
                         + Nếu nội dung liên quan tiền vào (vd: lương, thưởng) -> INCOME.
                         + Nếu nội dung liên quan tiêu dùng (vd: cafe, xăng, điện, nước) -> EXPENSE.
                    
                    2. QUY TẮC SỐ TIỀN:
                       - "k", "nghìn", "ng" -> 000
                       - "m", "tr", "triệu", "củ" -> 000000
                       - "lít" -> 00000 (trăm nghìn)
                       - "tỷ" -> 000000000
                    
                    3. DANH MỤC: Chọn tên trong danh sách khớp nhất. Nếu không, chọn "General".
                    
                    VÍ DỤ MẪU (HỌC THEO LOGIC NÀY):
                    User: "nhận lương 15tr" -> JSON: {"amount": 15000000, "type": "income", "category": "Salary", "description": "Lương tháng"}
                    User: "bán đồ cũ 500k" -> JSON: {"amount": 500000, "type": "income", "category": "Other Income", "description": "Bán đồ cũ"}
                    User: "cafe 30k" -> JSON: {"amount": 30000, "type": "expense", "category": "Food & Drinks", "description": "Cafe"} (Mặc định Expense vì là đồ uống)
                    User: "đóng tiền điện 1 củ" -> JSON: {"amount": 1000000, "type": "expense", "category": "Bills & Utilities", "description": "Tiền điện"}
                    User: "đổ xăng 50" -> JSON: {"amount": 50000, "type": "expense", "category": "Transportation", "description": "Đổ xăng"} (Hiểu ngầm 50 là 50k)
                    
                    CHỈ TRẢ VỀ JSON:
        """.trimIndent()
    }

    fun getScanBillPrompt(categoryNames: String, budgetCategories: String): String {
        return """
            You are a receipt data extraction tool. Read the bill image carefully.

            USER'S CATEGORIES: [$categoryNames]
            TRACKED BUDGETS (highest priority): [$budgetCategories]

            TASK: Return a JSON object with exactly these fields:
            - "amount": the final total as a plain number (see rules below)
            - "type": always "expense" unless it's a refund/cashback (then "income")
            - "category": the best matching category name (see rules below)
            - "description": a short English summary of the bill
            - "currency": the ISO currency code detected from the bill (see rules below)

            ── CURRENCY DETECTION RULES ──
            Look for currency symbols or codes on the bill:
            - "${'$'}" or "USD" or "Dollar"  → currency = "USD"
            - "₫" or "VND" or "đ" or "VN" → currency = "VND"
            - "€" or "EUR"                  → currency = "EUR"
            - "£" or "GBP"                  → currency = "GBP"
            - "¥" or "JPY" or "CNY"         → currency = "JPY" or "CNY"
            - If unclear, default to "VND"

            ── AMOUNT PARSING RULES (CRITICAL) ──
            The decimal separator differs by currency:
            - USD / EUR / GBP: period (.) is decimal → "6.33" = 6.33, "1,250.00" = 1250.0
            - VND: comma (,) is thousands separator, NO decimals → "633,000" = 633000, "1,250,000" = 1250000
            - NEVER treat a VND amount like "633,000" as 633.0
            - Always take the FINAL TOTAL / GRAND TOTAL line, not subtotals
            - Strip currency symbols, keep only the numeric value

            ── CATEGORY RULES ──
            1. TOP PRIORITY: If content matches any category in TRACKED BUDGETS → use that EXACT name (same case).
            2. Fallback: Pick the closest from USER'S CATEGORIES.
            3. Last resort: use "Other".

            ── EXAMPLES ──
            Restaurant bill ${'$'}6.33 USD → {"amount": 6.33, "type": "expense", "category": "Food & Drinks", "description": "Restaurant dinner", "currency": "USD"}
            Supermarket 350,000₫ VND    → {"amount": 350000, "type": "expense", "category": "Food & Drinks", "description": "Supermarket", "currency": "VND"}
            Gas station ${'$'}45.50 USD  → {"amount": 45.50, "type": "expense", "category": "Transportation", "description": "Gas", "currency": "USD"}
            Cafe 85,000đ                → {"amount": 85000, "type": "expense", "category": "Food & Drinks", "description": "Cafe", "currency": "VND"}

            RETURN ONLY VALID JSON, no explanation:
        """.trimIndent()
    }

    fun getChatAdvisorPrompt(context: String, userQuestion: String): String {
        return """
            Bạn là Trợ lý Tài chính (AI Financial Advisor).
            
            DỮ LIỆU TÀI CHÍNH CỦA NGƯỜI DÙNG:
            $context
            
            CÂU HỎI CỦA NGƯỜI DÙNG: "$userQuestion"
            
            NHIỆM VỤ:
            - Trả lời câu hỏi, đưa ra lời khuyên dựa trên số liệu trên.
            - Văn phong lịch sự, chuyên nghiệp, tiếng Việt tự nhiên.
            - KHÔNG trả về JSON. Trả về văn bản thường (Markdown) để dễ đọc.
            - Ngắn gọn, súc tích.
        """.trimIndent()
    }
}