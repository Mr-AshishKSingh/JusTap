package com.binay.shaw.justap.ui.mainScreens.settingsScreen

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.binay.shaw.justap.R
import com.binay.shaw.justap.adapter.SettingsItemAdapter
import com.binay.shaw.justap.base.BaseFragment
import com.binay.shaw.justap.base.ViewModelFactory
import com.binay.shaw.justap.data.LocalUserDatabase
import com.binay.shaw.justap.databinding.FragmentSettingsBinding
import com.binay.shaw.justap.databinding.OptionsModalBinding
import com.binay.shaw.justap.databinding.ParagraphModalBinding
import com.binay.shaw.justap.helper.Constants
import com.binay.shaw.justap.helper.ImageUtils
import com.binay.shaw.justap.helper.Util
import com.binay.shaw.justap.helper.Util.createBottomSheet
import com.binay.shaw.justap.helper.Util.setBottomSheet
import com.binay.shaw.justap.model.LocalUser
import com.binay.shaw.justap.model.SettingsItem
import com.binay.shaw.justap.ui.authentication.signInScreen.SignInScreen
import com.binay.shaw.justap.ui.mainScreens.MainActivity
import com.binay.shaw.justap.viewModel.LocalUserViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*


class SettingsFragment : BaseFragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var settingsItemList: ArrayList<SettingsItem>
    private lateinit var settingsItemAdapter: SettingsItemAdapter
    private lateinit var localUserDatabase: LocalUserDatabase
    private val localUserViewModel by viewModels<LocalUserViewModel> { ViewModelFactory() }
    private lateinit var feedback: ImageView
    private lateinit var localUser: LocalUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSettingsBinding.inflate(layoutInflater, container, false)

        initObservers()
        initialization()
        handleOperations()

        binding.toProfile.setOnClickListener {
            Navigation.findNavController(binding.root)
                .navigate(R.id.action_settings_to_profileFragment)
        }

        feedback.setOnClickListener {
            val openURL = Intent(Intent.ACTION_VIEW)
            openURL.data = Uri.parse(requireContext().resources.getString(R.string.mailTo))
            startActivity(openURL)
        }

        return binding.root
    }

    private fun handleOperations() {
        setupSettingsOptions()
    }

    private fun setupSettingsOptions() {
        /**set List*/
        settingsItemList = ArrayList()
        settingsItemList.apply {
            add(SettingsItem(1, R.drawable.edit_icon, "Edit profile", false))
            add(SettingsItem(3, R.drawable.scanner_icon, "Customize QR", false))
            add(SettingsItem(4, R.drawable.info_icon, "About us", false))
            add(SettingsItem(5, R.drawable.help_icon, "Need help?", false))
            add(SettingsItem(6, R.drawable.rate_icon, "Rate JusTap", false))
            add(SettingsItem(7, R.drawable.logout_icon, "Log out", false))
        }

        /**set find Id*/
        /**set Adapter*/
        settingsItemAdapter = SettingsItemAdapter(requireContext(), settingsItemList) {
            //Customize QR Listener
            when (it) {
                0 -> {
                    Navigation.findNavController(binding.root)
                        .navigate(R.id.action_settings_to_editProfileFragment)
                }

                1 -> {
//                    customizeQR()
                    gotoCustomizeQR()
                }

                2 -> {
                    val action = SettingsFragmentDirections.actionSettingsToResultFragment(
                        resultString = null,
                        isResult = false
                    )
                    Navigation.findNavController(binding.root).navigate(action)
                }
                3 -> {
                    needHelp()
                }
                4 -> {
                    openPlayStore()
                }
                5 -> {
                    logout()
                }
            }
        }
        /**setRecycler view Adapter*/
        binding.settingsRV.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = settingsItemAdapter
        }
    }

    private fun gotoCustomizeQR() {
        val profileImageIsPresent = localUser.userProfilePicture.isNullOrEmpty().not()
        val profileImage = if (profileImageIsPresent) binding.profileImage.drawable else null
        val profileBitmap = profileImage?.let { bitmap -> ImageUtils.getBitmapFromDrawable(bitmap) }
        val action =
            SettingsFragmentDirections.actionSettingsToCustomizeQRFragment(localUser, profileBitmap)
        findNavController().navigate(action)
    }

    private fun initObservers() {
        localUserViewModel.fetchUser.observe(viewLifecycleOwner) {
            it?.let {
                createUser(it)
            }

            val name = Util.getFirstName(localUser.userName)
            binding.settingsUserName.text = name

            val profileURL = localUser.userProfilePicture.toString()
            if (profileURL.isNotEmpty())
                Util.loadImagesWithGlide(binding.profileImage, profileURL)
        }
    }

    private fun createUser(user: LocalUser) {
        localUser = LocalUser(
            user.userID,
            user.userName,
            user.userEmail,
            user.userBio,
            user.userProfilePicture,
            user.userBannerPicture
        )
    }

    private fun needHelp() {
        val dialog = ParagraphModalBinding.inflate(layoutInflater)
        val bottomSheet = requireActivity().createBottomSheet()
        dialog.apply {
            paragraphHeading.text = resources.getString(R.string.welcome_to_justap)
            paragraphContent.text = resources.getString(R.string.needHelpSettingsDescription)
            lottieAnimationLayout.apply {
                setAnimation(R.raw.help_lottie)
                visibility = View.VISIBLE
            }
        }
        dialog.root.setBottomSheet(bottomSheet)
    }

    private fun openPlayStore() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(Constants.APP_URL)
        startActivity(intent)
    }


    @SuppressLint("SetTextI18n")
    private fun initialization() {

        (activity as MainActivity).supportActionBar?.hide()
        binding.include.toolbarTitle.text =
            requireContext().resources.getString(R.string.Settings)
        localUserDatabase = Room.databaseBuilder(
            requireContext(), LocalUserDatabase::class.java,
            "localDB"
        ).build()

        binding.include.apply {
            leftIcon.apply {
                feedback = this
                setImageResource(R.drawable.feedback_icon)
                visibility = View.VISIBLE
            }
        }
    }

    private fun logout() {

        val dialog = OptionsModalBinding.inflate(layoutInflater)
        val bottomSheet = requireContext().createBottomSheet()
        dialog.apply {

            optionsHeading.text = requireContext().resources.getString(R.string.LogoutTitle)
            optionsContent.text =
                requireContext().resources.getString(R.string.LogoutDescription)
            positiveOption.text = requireContext().resources.getString(R.string.LogoutTitle)
            positiveOption.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.negative_red
                )
            )

            negativeOption.text = requireContext().resources.getString(R.string.LogoutCancel)
            negativeOption.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.text_color
                )
            )

            positiveOption.setOnClickListener {
                bottomSheet.dismiss()
                clearDataAndLogout()
            }
            negativeOption.setOnClickListener {
                bottomSheet.dismiss()
                Util.log("Logout cancelled")
            }
        }
        dialog.root.setBottomSheet(bottomSheet)
    }

    private fun clearDataAndLogout() {
        lifecycleScope.launch(Dispatchers.Main) {
            val sharedPreferences =
                requireContext().getSharedPreferences(Constants.qrPref, Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.clear()
            editor.apply()
            val signOutFromFirebase =
                launch(Dispatchers.IO) { FirebaseAuth.getInstance().signOut() }
            signOutFromFirebase.join()
            LocalUserDatabase.getDatabase(requireContext()).clearTables()
            val intent = Intent(requireContext(), SignInScreen::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent).also { requireActivity().finish() }
            Util.log("Logged out")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}