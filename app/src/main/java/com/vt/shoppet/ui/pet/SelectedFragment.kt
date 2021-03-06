package com.vt.shoppet.ui.pet

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialContainerTransform
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ktx.toObjects
import com.vt.shoppet.R
import com.vt.shoppet.databinding.FragmentSelectedBinding
import com.vt.shoppet.model.Chat
import com.vt.shoppet.model.Pet
import com.vt.shoppet.model.User
import com.vt.shoppet.util.*
import com.vt.shoppet.viewmodel.DataViewModel
import com.vt.shoppet.viewmodel.FirestoreViewModel
import com.vt.shoppet.viewmodel.StorageViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SelectedFragment : Fragment(R.layout.fragment_selected) {

    private val binding by viewBinding(FragmentSelectedBinding::bind)

    private val firestore: FirestoreViewModel by viewModels()
    private val storage: StorageViewModel by viewModels()
    private val dataViewModel: DataViewModel by activityViewModels()

    private val progress by lazy { circularProgress }
    private val chat by lazy { getDrawable(R.drawable.ic_chat) }

    private var starred = false

    private fun starPetButton() =
        binding.btnStar.apply {
            isClickable = true
            setIconResource(R.drawable.ic_starred)
            text = getString(R.string.lbl_starred)
        }

    private fun unstarPetButton() =
        binding.btnStar.apply {
            isClickable = true
            setIconResource(R.drawable.ic_unstarred)
            text = getString(R.string.lbl_unstarred)
        }

    private fun removePetPhoto(id: String) {
        storage.removePetPhoto(id).observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                findNavController().previousBackStackEntry?.savedStateHandle?.set("removed", true)
                findNavController().popBackStack()
            }
            result.onFailure { exception ->
                binding.snackbar(message = exception.localizedMessage, owner = viewLifecycleOwner) {
                    removePetPhoto(id)
                }.show()
            }
        }
    }

    private fun soldDialog(id: String): AlertDialog {
        val fabChatSold = binding.fabChatSold
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_mark_as_sold)
            .setMessage(R.string.txt_mark_pet_sold)
            .setPositiveButton(R.string.btn_no, null)
            .setNegativeButton(R.string.btn_yes) { _, _ ->
                progress.start()
                fabChatSold.setImageDrawable(progress as Drawable)
                fabChatSold.isClickable = false
                firestore.markSoldPet(id).observe(viewLifecycleOwner) { result ->
                    result.onSuccess {
                        findNavController().run {
                            previousBackStackEntry?.savedStateHandle?.set("sold", true)
                            popBackStack()
                        }
                    }
                    result.onFailure { exception ->
                        binding.snackbar(
                            message = exception.localizedMessage,
                            owner = viewLifecycleOwner
                        ) {
                            soldDialog(id).show()
                        }.show()
                        fabChatSold.setImageDrawable(chat)
                        fabChatSold.isClickable = true
                        progress.stop()
                    }
                }
            }
            .create()
    }

    private fun removeDialog(id: String, image: String): AlertDialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_remove)
            .setMessage(R.string.txt_remove_pet)
            .setPositiveButton(R.string.btn_no, null)
            .setNegativeButton(R.string.btn_yes) { _, _ ->
                firestore.removePet(id).observe(viewLifecycleOwner) { result ->
                    result.onSuccess {
                        removePetPhoto(image)
                    }
                    result.onFailure { exception ->
                        binding.snackbar(
                            message = exception.localizedMessage,
                            owner = viewLifecycleOwner
                        ) {
                            removeDialog(id, image).show()
                        }.show()
                    }
                }
            }
            .create()
    }

    private fun checkStarredPet(id: String) {
        val btnStar = binding.btnStar
        progress.start()
        btnStar.isClickable = false
        btnStar.icon = progress as Drawable
        btnStar.text = getString(R.string.lbl_loading)
        firestore.checkStarredPet(id).observe(viewLifecycleOwner) { result ->
            result.onSuccess { document ->
                starred = document.exists()
                if (starred) {
                    starPetButton()
                    progress.stop()
                } else {
                    unstarPetButton()
                    progress.stop()
                }
            }
            result.onFailure { exception ->
                binding.snackbar(message = exception.localizedMessage, owner = viewLifecycleOwner) {
                    checkStarredPet(id)
                }.show()
                btnStar.isClickable = false
                progress.stop()
                unstarPetButton()
            }
        }
    }

    private fun createChat(chat: Chat) {
        firestore.createChat(chat).observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                dataViewModel.setChat(chat)
                val action =
                    SelectedFragmentDirections.actionSelectedToConversation(0, 1, chat.username[0])
                findNavController().navigate(action)
            }
            result.onFailure { exception ->
                binding.snackbar(message = exception.localizedMessage, owner = viewLifecycleOwner) {
                    createChat(chat)
                }.show()
            }
        }
    }

    private fun checkChat(pet: Pet, user: User) {
        val fabChatSold = binding.fabChatSold
        progress.start()
        fabChatSold.setImageDrawable(progress as Drawable)
        fabChatSold.isClickable = false
        firestore.checkChat(user.uid, pet.uid).observe(viewLifecycleOwner) { result ->
            result.onSuccess { snapshots ->
                if (snapshots.isEmpty) {
                    val chat = Chat(
                        id = "${user.uid}${pet.uid}",
                        uid = listOf(user.uid, pet.uid),
                        username = listOf(user.username, pet.username),
                        read = mutableListOf(true, true),
                        empty = true
                    )
                    createChat(chat)
                } else {
                    val chats: List<Chat> = snapshots.toObjects()
                    dataViewModel.setChat(chats.first())
                    val senderIndex = chats.first().uid.indexOf(user.uid)
                    val receiverIndex = chats.first().uid.indexOf(pet.uid)
                    val username = chats.first().username[receiverIndex]
                    val action =
                        SelectedFragmentDirections
                            .actionSelectedToConversation(senderIndex, receiverIndex, username)
                    findNavController().navigate(action)
                }
            }
            result.onFailure { exception ->
                binding.snackbar(message = exception.localizedMessage, owner = viewLifecycleOwner) {
                    checkChat(pet, user)
                }.show()
                fabChatSold.setImageDrawable(chat)
                fabChatSold.isClickable = true
                progress.stop()
            }
        }
    }

    private fun getUser(pet: Pet, user: User) {
        val imageSeller = binding.imageSeller
        firestore.getUserSnapshot(pet.uid).observe(viewLifecycleOwner) { result ->
            result.onSuccess { document ->
                val data: User = document.toObject()
                    ?: return@observe imageSeller.setImageResource(R.drawable.ic_person)
                dataViewModel.setUser(data)
                val image =
                    data.image ?: return@observe imageSeller.setImageResource(R.drawable.ic_person)
                loadProfileImage(imageSeller, storage.getUserPhoto(image))
            }
            result.onFailure { exception ->
                binding.snackbar(message = exception.localizedMessage, owner = viewLifecycleOwner) {
                    getUser(pet, user)
                }.show()
                imageSeller.setImageResource(R.drawable.ic_person)
            }
        }
    }

    private fun starPet(pet: Pet) {
        val btnStar = binding.btnStar
        pet.starred = true
        progress.start()
        btnStar.isClickable = false
        btnStar.icon = progress as Drawable
        btnStar.text = getString(R.string.lbl_loading)
        firestore.starPet(pet).observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                binding.snackbar(getString(R.string.txt_starred)).show()
                starred = true
                starPetButton()
                progress.stop()
            }
            result.onFailure { exception ->
                binding.snackbar(message = exception.localizedMessage, owner = viewLifecycleOwner) {
                    starPet(pet)
                }.show()
                starred = false
                unstarPetButton()
                progress.stop()
            }
        }
    }

    private fun unstarPet(id: String) {
        val btnStar = binding.btnStar
        progress.start()
        btnStar.isClickable = false
        btnStar.icon = progress as Drawable
        btnStar.text = getString(R.string.lbl_loading)
        firestore.unstarPet(id).observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                binding.snackbar(getString(R.string.txt_unstarred)).show()
                starred = false
                unstarPetButton()
                progress.stop()
            }
            result.onFailure { exception ->
                binding.snackbar(message = exception.localizedMessage, owner = viewLifecycleOwner) {
                    unstarPet(id)
                }.show()
                starred = true
                starPetButton()
                progress.stop()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.fragment
            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
            interpolator = FastOutSlowInInterpolator()
            duration = 500
            isElevationShadowEnabled = false
        }
        sharedElementReturnTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.fragment
            fadeMode = MaterialContainerTransform.FADE_MODE_THROUGH
            interpolator = FastOutSlowInInterpolator()
            duration = 500
            isElevationShadowEnabled = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnStar = binding.btnStar
        val imagePet = binding.imagePet
        val fabChatSold = binding.fabChatSold
        val cardSeller = binding.cardSeller

        val btnGroup = binding.btnGrp
        val btnEdit = binding.btnEdit
        val btnRemove = binding.btnRemove
        val txtName = binding.txtName
        val txtPrice = binding.txtPrice
        val txtType = binding.txtType
        val txtSex = binding.txtSex
        val txtAge = binding.txtAge
        val txtBreed = binding.txtBreed
        val layoutCatsDogs = binding.layoutCatsDogs
        val txtVaccineStatus = binding.txtVaccineStatus
        val txtMedicalRecords = binding.txtMedicalRecords
        val txtDescription = binding.txtDescription
        val txtDate = binding.txtDate
        val txtSeller = binding.txtSeller

        val types = resources.getStringArray(R.array.type)

        val savedStateHandle = findNavController().currentBackStackEntry?.savedStateHandle
        savedStateHandle?.getLiveData<Boolean>("edited")?.observe(viewLifecycleOwner) { edited ->
            if (edited) binding.snackbar(getString(R.string.txt_pet_updated)).show()
            savedStateHandle.remove<Boolean>("edited")
        }

        dataViewModel.currentPet.observe(viewLifecycleOwner) { pet ->
            binding.root.transitionName = pet.id
            layoutCatsDogs.isVisible = pet.type == types[1] || pet.type == types[2]
            fabChatSold.isGone = pet.sold

            loadFirebaseImage(imagePet, storage.getPetPhoto(pet.image))

            txtName.text = pet.name
            val price = "${getString(R.string.sym_currency)} ${pet.price}"
            txtPrice.text = price
            txtType.text = pet.type
            txtSex.text = pet.sex
            txtAge.text = pet.dateOfBirth.calculateAge
            txtBreed.text = pet.breed
            txtVaccineStatus.text = pet.vaccineStatus
            txtMedicalRecords.text = pet.medicalRecords
            txtDescription.text = pet.description
            txtDate.text = pet.date.calculatePostDuration(pet.sold)
            txtSeller.text = pet.username

            dataViewModel.currentUser.observe(viewLifecycleOwner) { user ->
                if (user.uid == pet.uid) {
                    btnGroup.isVisible = true
                    fabChatSold.setImageResource(R.drawable.ic_check)
                } else {
                    btnStar.isVisible = true
                    fabChatSold.setImageResource(R.drawable.ic_chat)
                    checkStarredPet(pet.id)
                }

                fabChatSold.setOnClickListener {
                    if (pet.uid == user.uid) soldDialog(pet.id).show()
                    else checkChat(pet, user)
                }

                cardSeller.setOnClickListener {
                    val action =
                        SelectedFragmentDirections.actionSelectedToProfile(pet.uid == user.uid)
                    findNavController().navigate(action)
                }

                getUser(pet, user)
            }

            btnStar.setOnClickListener {
                if (starred) unstarPet(pet.id)
                else starPet(pet)
            }

            btnRemove.setOnClickListener {
                btnGroup.clearChecked()
                removeDialog(pet.id, pet.image).show()
            }

            btnEdit.setOnClickListener {
                btnGroup.clearChecked()
                findNavController().navigate(R.id.action_selected_to_edit_pet)
            }
        }
    }

}