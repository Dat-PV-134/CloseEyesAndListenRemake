package com.rekoj134.closeeyesandlisten.ui.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.rekoj134.closeeyesandlisten.base.BaseActivity
import com.rekoj134.closeeyesandlisten.constant.Constants
import com.rekoj134.closeeyesandlisten.remake.R
import com.rekoj134.closeeyesandlisten.remake.databinding.ActivityLoginAndSignUpBinding
import com.rekoj134.closeeyesandlisten.ui.fragment.LoginFragment
import com.rekoj134.closeeyesandlisten.ui.fragment.SignUpFragment

class LoginAndSignUpActivity : BaseActivity() {
    private lateinit var binding: ActivityLoginAndSignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginAndSignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupView()
    }

    private fun setupView() {
        if (intent?.action == Constants.ACTION_LOG_IN) replaceFragment(LoginFragment.newInstance())
        else replaceFragment(SignUpFragment.newInstance())
    }

    fun goToLogin() {
        replaceFragment(LoginFragment.newInstance())
    }

    fun goToSignUp() {
        replaceFragment(SignUpFragment.newInstance())
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}