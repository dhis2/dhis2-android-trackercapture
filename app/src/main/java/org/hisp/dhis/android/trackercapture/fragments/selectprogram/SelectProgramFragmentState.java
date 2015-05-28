package org.hisp.dhis.android.trackercapture.fragments.selectprogram;

import android.os.Parcel;
import android.os.Parcelable;


class SelectProgramFragmentState implements Parcelable {
    public static final Creator<SelectProgramFragmentState> CREATOR
            = new Creator<SelectProgramFragmentState>() {

        public SelectProgramFragmentState createFromParcel(Parcel in) {
            return new SelectProgramFragmentState(in);
        }

        public SelectProgramFragmentState[] newArray(int size) {
            return new SelectProgramFragmentState[size];
        }
    };
    private static final String TAG = SelectProgramFragmentState.class.getName();
    private boolean syncInProcess;

    private String orgUnitLabel;
    private String orgUnitId;

    private String programName;
    private String programId;


    public SelectProgramFragmentState() {
    }

    public SelectProgramFragmentState(SelectProgramFragmentState state) {
        if (state != null) {
            setSyncInProcess(state.isSyncInProcess());
            setOrgUnit(state.getOrgUnitId(), state.getOrgUnitLabel());
            setProgram(state.getProgramId(), state.getProgramName());
        }
    }

    private SelectProgramFragmentState(Parcel in) {
        syncInProcess = in.readInt() == 1;

        orgUnitLabel = in.readString();
        orgUnitId = in.readString();

        programName = in.readString();
        programId = in.readString();
    }

    @Override
    public int describeContents() {
        return TAG.length();
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeInt(syncInProcess ? 1 : 0);

        parcel.writeString(orgUnitLabel);
        parcel.writeString(orgUnitId);

        parcel.writeString(programName);
        parcel.writeString(programId);

    }

    public boolean isSyncInProcess() {
        return syncInProcess;
    }

    public void setSyncInProcess(boolean syncInProcess) {
        this.syncInProcess = syncInProcess;
    }

    public void setOrgUnit(String orgUnitId, String orgUnitLabel) {
        this.orgUnitId = orgUnitId;
        this.orgUnitLabel = orgUnitLabel;
    }

    public void resetOrgUnit() {
        orgUnitId = null;
        orgUnitLabel = null;
    }

    public boolean isOrgUnitEmpty() {
        return (orgUnitId == null || orgUnitLabel == null);
    }

    public String getOrgUnitLabel() {
        return orgUnitLabel;
    }

    public String getOrgUnitId() {
        return orgUnitId;
    }

    public void setProgram(String programId, String programLabel) {
        this.programId = programId;
        this.programName = programLabel;
    }

    public void resetProgram() {
        programId = null;
        programName = null;
    }

    public boolean isProgramEmpty() {
        return (programId == null || programName == null);
    }

    public String getProgramName() {
        return programName;
    }

    public String getProgramId() {
        return programId;
    }


}

