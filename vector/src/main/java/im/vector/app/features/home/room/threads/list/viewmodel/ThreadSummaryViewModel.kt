/*
 * Copyright 2021 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.app.features.home.room.threads.list.viewmodel

import com.airbnb.mvrx.FragmentViewModelContext
import com.airbnb.mvrx.MavericksViewModelFactory
import com.airbnb.mvrx.ViewModelContext
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import im.vector.app.core.platform.EmptyAction
import im.vector.app.core.platform.EmptyViewEvents
import im.vector.app.core.platform.VectorViewModel
import im.vector.app.features.home.room.threads.list.views.ThreadListFragment
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.flow.flow

class ThreadSummaryViewModel @AssistedInject constructor(@Assisted val initialState: ThreadSummaryViewState,
                                                         private val session: Session) :
        VectorViewModel<ThreadSummaryViewState, EmptyAction, EmptyViewEvents>(initialState) {

    private val room = session.getRoom(initialState.roomId)

    @AssistedFactory
    interface Factory {
        fun create(initialState: ThreadSummaryViewState): ThreadSummaryViewModel
    }

    companion object : MavericksViewModelFactory<ThreadSummaryViewModel, ThreadSummaryViewState> {

        @JvmStatic
        override fun create(viewModelContext: ViewModelContext, state: ThreadSummaryViewState): ThreadSummaryViewModel? {
            val fragment: ThreadListFragment = (viewModelContext as FragmentViewModelContext).fragment()
            return fragment.threadSummaryViewModelFactory.create(state)
        }
    }

    init {
        observeThreadsList(initialState.shouldFilterThreads)
    }

    override fun handle(action: EmptyAction) {}

    private fun observeThreadsList(shouldFilterThreads: Boolean) =
            room?.flow()
                    ?.liveThreadList()
                    ?.map {
                        if (!shouldFilterThreads) return@map it
                        it.filter { timelineEvent ->
                            room.isUserParticipatingInThread(timelineEvent.eventId, session.myUserId)
                        }
                    }
                    ?.flowOn(room.coroutineDispatchers.io)
                    ?.execute { asyncThreads ->
                        copy(
                                rootThreadEventList = asyncThreads,
                                shouldFilterThreads = shouldFilterThreads)
                    }

    fun applyFiltering(shouldFilterThreads: Boolean) {
        observeThreadsList(shouldFilterThreads)
    }
}