package ltd.lths.wireless.ghinf.ap.other

import java.text.SimpleDateFormat
import java.util.*

/**
 * ScanGhinfAP
 * ltd.lths.wireless.ghinf.ap.other.CEE
 *
 * @author Score2
 * @since 2022/05/25 0:22
 */
object CEE {

    val lastDay: Int get() {
        val daynow = SimpleDateFormat("D")
        val a = Date()
        val dayns = daynow.format(a)
        //获取了今天是一年中第几天
        //获取了今天是一年中第几天
        val day = dayns.toInt()
        //将String类型转换为int类型
        //将String类型转换为int类型
        val yearnow = SimpleDateFormat("yyyy")
        val yeara = Date()
        val yearns = yearnow.format(yeara)
        //获取今年是哪一年
        //获取今年是哪一年
        val year = yearns.toInt()
        //将String类型转换为int类型
        //将String类型转换为int类型
        if (year % 4 == 0 && year % 100 != 0 || year % 400 == 0) {
            //如果是闰年
            if (day < 159) {
                return 159 - day
            } else if (day == 159 || day == 200) {
                //闰年高考为一年的159天和200天
                return 0
            } else if (day > 200) {
                return 524 - day
            }
        //如果是平年
        } else if (day < 158) {
                return 158 - day
        } else if (day == 158 || day == 159) {
            //平年高考为一年的158天和159天
            return 0
        } else if (day > 159) {
            return 523 - day
        }
        return 666
    }

}