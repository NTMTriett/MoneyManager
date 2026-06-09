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
                         + Nếu nội dung liên quan tiền vào (vd: lương, thưởng, bán đồ cũ, bán đồ mới, kinh doanh) -> INCOME.
                         + Nếu nội dung liên quan tiêu dùng (vd: cafe, xăng, điện, nước, quần áo, siêu thị, ) -> EXPENSE.
                    
                    2. QUY TẮC SỐ TIỀN:
                       - KIỂM TRA ĐƠN VỊ:
                        + Nếu thấy "$", "usd", "đô" -> "currency": "USD", giữ nguyên số tiền (100$ = 100). KHÔNG nhân thêm nghìn.
                        + Các trường hợp khác -> "currency": "VND".
                        
                       - NẾU LÀ VND (QUY ĐỔI):
                       - "k", "nghìn", "ng" -> 000
                       - "m", "tr", "triệu", "củ" -> 000000
                       - "lít" -> 00000 (trăm nghìn)
                       - "tỷ" -> 000000000
                       - "xị" -> 00000 (100,000)
                       - "chai" -> 000000 (1,000,000)
                    
                       - Cho phép số thập phân:
                         + "1.5tr" = 1,500,000
                         + "2.3k" = 2,300
                         + "0.5 củ" = 500,000
                    
                       - Cho phép dạng kết hợp:
                         + "1tr500k" = 1,500,000
                         + "2 triệu 300k" = 2,300,000
                         + "1 tỷ 200 triệu" = 1,200,000,000
                    
                       - Cho phép viết liền hoặc có khoảng cách:
                         + "200k", "200 k", "200K" đều hợp lệ
                    
                       - Bỏ qua ký hiệu tiền:
                         + "đ", "₫", "vnd"
                    
                       - Hỗ trợ định dạng số:
                         + "1,000,000" = 1000000
                         + "1.000.000" = 1000000
                    
                       - Số không có đơn vị:
                         + Nếu nhỏ (vd: 50, 100, 200) -> hiểu là nghìn (50 = 50,000)
                         + Nếu lớn (>= 1000) -> giữ nguyên
                    
                       - Hỗ trợ số âm:
                         + "-200k", "200k-" = -200,000
                   
                    3. DANH MỤC: Chọn tên trong danh sách khớp nhất. Nếu không, chọn "General".
                    
                    VÍ DỤ MẪU (HỌC THEO LOGIC NÀY):
                    User: "nhận lương 15tr" -> JSON: {"amount": 15000000, "type": "income", "category": "Salary", "description": "Lương tháng"}
                    User: "bán đồ cũ 500k" -> JSON: {"amount": 500000, "type": "income", "category": "Other Income", "description": "Bán đồ cũ"}
                    User: "cafe 30k" -> JSON: {"amount": 30000, "type": "expense", "category": "Food & Drinks", "description": "Cafe"} (Mặc định Expense vì là đồ uống)
                    User: "đóng tiền điện 1 củ" -> JSON: {"amount": 1000000, "type": "expense", "category": "Bills & Utilities", "description": "Tiền điện"}
                    User: "đổ xăng 50" -> JSON: {"amount": 50000, "type": "expense", "category": "Transportation", "description": "Đổ xăng"} (Hiểu ngầm 50 là 50k)
                    User: "mua xe 650 triệu" -> JSON: {"amount": 650000000, "type": "expense", "category": "Transportation", "description": "Mua xe"}
                    User: "bán đất 2 tỷ" -> JSON: {"amount": 2000000000, "type": "income", "category": "Other Income", "description": "Bán đất"}
                    User: "mua căn hộ 1.8 tỷ" -> JSON: {"amount": 1800000000, "type": "expense", "category": "Housing", "description": "Mua căn hộ"}
                    User: "mua nhà hết 50 tỷ" -> JSON: {"amount": 50000000000, "type": "expense", "category": "Housing", "description": "Mua nhà"}
                    User: "bán biệt thự 75 tỷ" -> JSON: {"amount": 75000000000, "type": "income", "category": "Other Income", "description": "Bán biệt thự"}
                    User: "đóng tiền điện 1 củ" -> JSON: {"amount": 1000000, "type": "expense", "category": "Bills & Utilities", "description": "Tiền điện"}
                    CHỈ TRẢ VỀ JSON:
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