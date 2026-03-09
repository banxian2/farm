package com.farm.seeker.utils

import com.farm.seeker.R

object AvatarUtils {

    val maleAvatars = listOf(
        R.drawable.avatar_m_1,
        R.drawable.avatar_m_2,
        R.drawable.avatar_m_3,
        R.drawable.avatar_m_4,
        R.drawable.avatar_m_5,
        R.drawable.avatar_m_6,
        R.drawable.avatar_m_7,
        R.drawable.avatar_m_8,
        R.drawable.avatar_m_9
    )

    val femaleAvatars = listOf(
        R.drawable.avatar_f_1,
        R.drawable.avatar_f_2,
        R.drawable.avatar_f_3,
        R.drawable.avatar_f_4,
        R.drawable.avatar_f_5,
        R.drawable.avatar_f_6,
        R.drawable.avatar_f_7,
        R.drawable.avatar_f_8,
        R.drawable.avatar_f_9
    )

    fun getAvatarResId(index: Int): Int {
        return when {
            index in 1..9 -> maleAvatars.getOrElse(index - 1) { R.drawable.ic_me }
            index in 11..19 -> femaleAvatars.getOrElse(index - 11) { R.drawable.ic_me }
            else -> R.drawable.ic_me
        }
    }

    fun getAvatarIndex(resId: Int): Int {
        val maleIndex = maleAvatars.indexOf(resId)
        if (maleIndex != -1) return maleIndex + 1

        val femaleIndex = femaleAvatars.indexOf(resId)
        if (femaleIndex != -1) return femaleIndex + 11

        return 0
    }
}
