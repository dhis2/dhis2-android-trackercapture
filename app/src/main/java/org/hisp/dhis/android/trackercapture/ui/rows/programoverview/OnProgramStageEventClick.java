/*
 *  Copyright (c) 2016, University of Oslo
 *  * All rights reserved.
 *  *
 *  * Redistribution and use in source and binary forms, with or without
 *  * modification, are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright notice, this
 *  * list of conditions and the following disclaimer.
 *  *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *  * this list of conditions and the following disclaimer in the documentation
 *  * and/or other materials provided with the distribution.
 *  * Neither the name of the HISP project nor the names of its contributors may
 *  * be used to endorse or promote products derived from this software without
 *  * specific prior written permission.
 *  *
 *  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package org.hisp.dhis.android.trackercapture.ui.rows.programoverview;

import android.view.View;
import android.widget.ImageButton;

import org.hisp.dhis.android.sdk.events.OnRowClick;
import org.hisp.dhis.android.sdk.persistence.models.Event;

/**
 * Created by erling on 5/28/15.
 */
public class OnProgramStageEventClick extends OnRowClick<Event>
{
    private final Event event;
    private final ImageButton hasFailedButton;
    private final boolean hasPressedFailedButton;
    private final String errorMessage;
    private final boolean isLongPressed;
    private final View view;

    public OnProgramStageEventClick(Event event, ImageButton hasFailedButton,
            boolean hasPressedFailedButton, String errorMessage, ITEM_STATUS status,
            boolean isLongPressed, View view)
    {
        super(status, event);
        this.event = event;
        this.hasFailedButton = hasFailedButton;
        this.hasPressedFailedButton = hasPressedFailedButton;
        this.errorMessage = errorMessage;
        this.isLongPressed = isLongPressed;
        this.view = view;
    }
    public boolean isHasPressedFailedButton() {
        return hasPressedFailedButton;
    }

    public ImageButton getHasFailedButton() {
        return hasFailedButton;
    }

    public Event getEvent() {
        return event;
    }
    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isLongPressed() {
        return isLongPressed;
    }

    public View getView() {
        return view;
    }
}
