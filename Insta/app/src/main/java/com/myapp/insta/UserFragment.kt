package com.myapp.insta


import android.os.Bundle
import android.support.v4.app.Fragment //v4가 아닌 그냥 app은 더 이상 지원을 안함
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


class UserFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        return LayoutInflater.from(inflater.context)
                .inflate(R.layout.fragment_user, container, false)
    }


}
