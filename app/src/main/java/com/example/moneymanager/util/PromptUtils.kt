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
            Nhiệm vụ của bạn là phân tích INPUT và trả về một đối tượng JSON.
            
            DANH MỤC HỆ THỐNG: Food & Drinks, Transport, Shopping, Bills & Utilities, General, Salary, Bonus, Other Income, Housing, Education, Health, Entertainment, Personal Care, Gifts, Debt/Loan, Investments, Savings, Insurance
            (Thêm các danh mục của người dùng nếu có: $categoryNames)
            
            VÍ DỤ CÁC DANH MỤC CHÍNH:
            - Food & Drinks: Cafe, Ăn trưa, Bữa tối, Đồ ăn vặt, Siêu thị (nếu là mua đồ ăn), Nước uống, Đồ uống.
            - Transport: Xăng xe, Đi lại, Grab, Taxi, Vé tàu.
            - Bills & Utilities: Tiền điện, Tiền nước, Internet, Tiền nhà (nếu là thuê), Tiền điện thoại.
            - Shopping: Quần áo, Giày dép, Mỹ phẩm, Đồ điện tử, Sách.
            - General: Các khoản chi tiêu nhỏ, không rõ ràng, linh tinh.
            - Salary: Tiền lương.
            - Other Income: Tiền thưởng, Bán đồ cũ, Lãi tiết kiệm, Được cho.
            - Housing: Tiền thuê nhà, Tiền mua nhà, Sửa chữa nhà.

            
            INPUT: "$input"
            
            NHIỆM VỤ: Trả về JSON với các trường: "amount", "type", "category", "description".
            
            1. QUY TẮC PHÂN LOẠI "type" (Rất quan trọng):
               - INCOME (Thu nhập): Nếu câu chứa từ khóa "nhận", "lương", "thưởng", "lãi", "bán", "được cho", "biếu", "hoàn tiền", "thu", hoặc các khoản tiền vào.
               - EXPENSE (Chi tiêu): Nếu câu chứa "mua", "trả", "đóng", "nạp", "đi" (chợ/xe), "ăn", "uống", "sắm", "tốn", "chi", hoặc các khoản tiền ra.
               - MẶC ĐỊNH (Nếu không rõ động từ): Nếu nội dung liên quan tiền vào (vd: lương, thưởng, bán đồ cũ, kinh doanh) -> INCOME. Nếu nội dung liên quan tiêu dùng (vd: cafe, xăng, điện, nước, quần áo, siêu thị) -> EXPENSE.
            
            2. QUY TẮC SỐ TIỀN (Rất quan trọng - ưu tiên đơn vị tiền tệ):
               - Ưu tiên 1: ĐƠN VỊ TIỀN TỆ CỤ THỂ
                 - Nếu có "$", "usd", "đô": "currency": "USD", giữ nguyên số tiền, KHÔNG ÁP DỤNG QUY ĐỔI VND hay quy tắc số không đơn vị. Ví dụ: "5$" -> 5.0.
                 - Mặc định: "currency": "VND".
               
               - Ưu tiên 2: QUY ĐỔI ĐƠN VỊ VND (Chỉ áp dụng khi không phải USD):
                 - "k", "nghìn", "ng" -> 000 (vd: 100k = 100000).
                 - "m", "tr", "triệu", "củ", "chai" -> 000000 (vd: 1tr = 1000000).
                 - "tỷ" -> 000000000.
                 - "xị" -> 50000 (50 nghìn) hoặc 100000 (100 nghìn) tùy ngữ cảnh. Cần cân nhắc thêm.
               - Số thập phân: Hỗ trợ (vd: 1.5tr = 1,500,000).
               - Kết hợp: Hỗ trợ (vd: 1tr500k = 1,500,000).
               - Viết liền/cách: "200k", "200 k", "200K" đều hợp lệ.
               - Bỏ qua ký hiệu tiền: "đ", "₫", "vnd".
               - Số không có đơn vị (Chỉ áp dụng khi không phải USD và không có đơn vị VND rõ ràng):
                 - Nếu nhỏ (vd: 50, 100, 200) -> hiểu là nghìn. Nếu lớn (>= 1000) -> giữ nguyên.
               - Số âm: Hỗ trợ (vd: "-200k", "200k-").
                   
            3. DANH MỤC: Chọn tên trong "DANH MỤC HỆ THỐNG" (bao gồm cả danh mục người dùng) khớp nhất. Nếu không có danh mục nào khớp sát, chọn "General".
            
            VÍ DỤ MẪU (HỌC THEO LOGIC NÀY VÀ ƯU TIÊN DANH MỤC GỢI Ý):
            User: "nhận lương 15tr" -> JSON: {"amount": 15000000, "type": "income", "category": "Salary", "description": "Lương tháng"}
            User: "bán đồ cũ 500k" -> JSON: {"amount": 500000, "type": "income", "category": "Other Income", "description": "Bán đồ cũ"}
            User: "cafe 30k" -> JSON: {"amount": 30000, "type": "expense", "category": "Food & Drinks", "description": "Cafe"}
            User: "ăn trưa 100k" -> JSON: {"amount": 100000, "type": "expense", "category": "Food & Drinks", "description": "Ăn trưa"}
            User: "5$ lunch" -> JSON: {"amount": 5.0, "type": "expense", "category": "Food & Drinks", "description": "Lunch"}
            User: "mua quần áo 200k" -> JSON: {"amount": 200000, "type": "expense", "category": "Shopping", "description": "Mua quần áo"}
            User: "đổ xăng 50" -> JSON: {"amount": 50000, "type": "expense", "category": "Transport", "description": "Đổ xăng"}
            User: "đóng tiền điện 1 củ" -> JSON: {"amount": 1000000, "type": "expense", "category": "Bills & Utilities", "description": "Tiền điện"}
            User: "chi phí đi lại 120k" -> JSON: {"amount": 120000, "type": "expense", "category": "Transport", "description": "Chi phí đi lại"}
            User: "học phí tháng này 5tr" -> JSON: {"amount": 5000000, "type": "expense", "category": "Education", "description": "Học phí tháng"}
            User: "mua sách 75k" -> JSON: {"amount": 75000, "type": "expense", "category": "Shopping", "description": "Mua sách"}
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