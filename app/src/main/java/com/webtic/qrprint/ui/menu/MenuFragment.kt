package com.webtic.qrprint.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.webtic.qrprint.R
import com.webtic.qrprint.ui.BaseFragment
import com.webtic.qrprint.util.ConnectionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MenuFragment : BaseFragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().supportFragmentManager.fragments.removeAt(0)
        initNavigation(view)
    }

    private fun initNavigation(view: View) {
        view.findViewById<Button>(R.id.tasksBtn).setOnClickListener {
            findNavController().navigate(MenuFragmentDirections.actionMenuFragmentToTasksFragment())
        }

        view.findViewById<Button>(R.id.billsBtn).setOnClickListener {
            findNavController().navigate(MenuFragmentDirections.actionMenuFragmentToBillsFragment())
        }

        view.findViewById<Button>(R.id.liveOrdersBtn).setOnClickListener {
            findNavController().navigate(MenuFragmentDirections.actionMenuFragmentToLiveOrdersFragment())
        }

        view.findViewById<Button>(R.id.deliveryNotesBtn).setOnClickListener {
            findNavController().navigate(MenuFragmentDirections.actionMenuFragmentToDeliveryNotesFragment())
        }

        view.findViewById<Button>(R.id.revenuesBtn).setOnClickListener {
            findNavController().navigate(MenuFragmentDirections.actionMenuFragmentToRevenuesFragment())
        }

        view.findViewById<Button>(R.id.searchBtn).setOnClickListener {
            findNavController().navigate(
                MenuFragmentDirections.actionMenuFragmentToSearchFragment(
                    null
                )
            )
        }

        view.findViewById<Button>(R.id.qrBtn).setOnClickListener {
            findNavController().navigate(
                MenuFragmentDirections.actionMenuFragmentToQrCodeFragment(
                    null
                )
            )
        }

    }
}