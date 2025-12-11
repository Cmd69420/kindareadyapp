package com.bluemix.clients_lead.di

import com.bluemix.clients_lead.data.repository.MeetingRepositoryImpl
import com.bluemix.clients_lead.domain.repository.IMeetingRepository
import com.bluemix.clients_lead.domain.usecases.*
import com.bluemix.clients_lead.features.meeting.vm.MeetingViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val meetingModule = module {

    // Repository
    single<IMeetingRepository> {
        MeetingRepositoryImpl(get())
    }

    // Use Cases
    factory { StartMeeting(get()) }
    factory { EndMeeting(get()) }
    factory { GetActiveMeetingForClient(get()) }
    factory { GetUserMeetings(get()) }
    factory { UploadMeetingAttachment(get()) }

    // ViewModel
    viewModel {
        MeetingViewModel(
            startMeeting = get(),
            endMeeting = get(),
            getActiveMeetingForClient = get(),
            uploadMeetingAttachment = get(),
            getCurrentUserId = get(),
            context = androidContext()
        )
    }
}