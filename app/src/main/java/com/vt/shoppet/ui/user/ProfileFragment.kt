package com.vt.shoppet.ui.user

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.vt.shoppet.R
import com.vt.shoppet.databinding.FragmentProfileBinding
import com.vt.shoppet.model.User
import com.vt.shoppet.ui.MainActivity
import com.vt.shoppet.util.*
import com.vt.shoppet.viewmodel.AuthViewModel
import com.vt.shoppet.viewmodel.DataViewModel
import com.vt.shoppet.viewmodel.FirestoreViewModel
import com.vt.shoppet.viewmodel.StorageViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
@ExperimentalCoroutinesApi
class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val binding by viewBinding(FragmentProfileBinding::bind)

    private val args: ProfileFragmentArgs by navArgs()

    private val auth: AuthViewModel by activityViewModels()
    private val firestore: FirestoreViewModel by activityViewModels()
    private val storage: StorageViewModel by activityViewModels()
    private val dataViewModel: DataViewModel by activityViewModels()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var progress: Animatable
    private lateinit var report: Drawable

    private fun reportUser(uid: String, currentUid: String) {
        progress.start()
        toolbar.menu.getItem(0).icon = progress as Drawable
        firestore.reportUser(uid, currentUid).observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                toolbar.menu.getItem(0).icon = report
                progress.stop()
                showSnackbar(getString(R.string.txt_reported_user))
            }
            result.onFailure { exception ->
                showActionSnackbar(exception) {
                    reportUser(uid, currentUid)
                }
                toolbar.menu.getItem(0).icon = report
                progress.stop()
            }
        }
    }

    private fun addReport(uid: String, currentUid: String) {
        progress.start()
        toolbar.menu.getItem(0).icon = progress as Drawable
        firestore.addReport(uid).observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                reportUser(uid, currentUid)
            }
            result.onFailure { exception ->
                showActionSnackbar(exception) {
                    addReport(uid, currentUid)
                }
                toolbar.menu.getItem(0).icon = report
                progress.stop()
            }
        }
    }

    private fun dialog(uid: String, currentUid: String) =
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_report_user)
            .setMessage(R.string.txt_report_user)
            .setPositiveButton(R.string.btn_confirm) { _, _ ->
                addReport(uid, currentUid)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .create()

    private fun getReport(uid: String, currentUid: String) {
        toolbar.menu.getItem(0).icon = progress as Drawable
        firestore.getReport(uid, currentUid).observe(viewLifecycleOwner) { result ->
            result.onSuccess { document ->
                if (document.exists()) {
                    showSnackbar(getString(R.string.txt_user_already_reported))
                    toolbar.menu.getItem(0).setIcon(R.drawable.ic_report)
                } else dialog(uid, currentUid).show()
            }
            result.onFailure { exception ->
                showActionSnackbar(exception) {
                    getReport(uid, currentUid)
                }
                toolbar.menu.getItem(0).setIcon(R.drawable.ic_report)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as MainActivity

        toolbar = activity.binding.toolbar
        progress = circularProgress()
        report = getDrawable(R.drawable.ic_report)

        val currentUid = auth.uid()
        val email = auth.email()

        val imageUser = binding.imageUser
        val txtName = binding.txtName
        val txtEmailTitle = binding.txtEmailTitle
        val txtEmail = binding.txtEmail
        val txtUsername = binding.txtUsername
        val txtMobile = binding.txtMobile
        val txtLocation = binding.txtLocation
        val txtSex = binding.txtSex
        val txtDateOfBirthTitle = binding.txtDateOfBirthTitle
        val txtDateOfBirth = binding.txtDateOfBirth

        val dateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")

        fun setProfile(user: User) {
            val uid = user.uid
            txtName.text = user.name
            if (args.current) {
                txtEmailTitle.isVisible = true
                txtEmail.isVisible = true
                txtEmail.text = email
                txtDateOfBirthTitle.isVisible = true
                txtDateOfBirth.isVisible = true
            }
            txtUsername.text = user.username
            txtMobile.text = user.mobile.replaceFirst("0", "+63")
            txtLocation.text = user.location
            txtSex.text = user.sex
            val dateOfBirth =
                LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(user.dateOfBirth.seconds),
                    localZoneId
                )
            txtDateOfBirth.text = dateTimeFormatter.format(dateOfBirth)

            val image = user.image
            if (image != null) {
                loadProfileImage(imageUser, storage.getUserPhoto(image))
            } else imageUser.setImageResource(R.drawable.ic_person)

            toolbar.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.item_edit -> {
                        findNavController().navigate(R.id.action_profile_to_edit_profile)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.item_report -> {
                        getReport(uid, currentUid)
                        return@setOnMenuItemClickListener true
                    }
                    else -> return@setOnMenuItemClickListener false
                }
            }
        }

        if (args.current) {
            toolbar.inflateMenu(R.menu.menu_current_profile)
            dataViewModel.currentUser.observe(viewLifecycleOwner) { user ->
                setProfile(user)
            }
        } else {
            toolbar.inflateMenu(R.menu.menu_profile)
            dataViewModel.user.observe(viewLifecycleOwner) { user ->
                setProfile(user)
            }
        }
    }

}