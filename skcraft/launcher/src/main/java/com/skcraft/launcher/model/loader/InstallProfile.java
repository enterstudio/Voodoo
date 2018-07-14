// Generated by delombok at Sat Jul 14 04:26:21 CEST 2018
/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */
package com.skcraft.launcher.model.loader;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InstallProfile {
    @JsonProperty("install")
    private InstallData installData;
    private VersionInfo versionInfo;

    @SuppressWarnings("all")
    public InstallProfile() {
    }

    @SuppressWarnings("all")
    public InstallData getInstallData() {
        return this.installData;
    }

    @SuppressWarnings("all")
    public VersionInfo getVersionInfo() {
        return this.versionInfo;
    }

    @SuppressWarnings("all")
    public void setInstallData(final InstallData installData) {
        this.installData = installData;
    }

    @SuppressWarnings("all")
    public void setVersionInfo(final VersionInfo versionInfo) {
        this.versionInfo = versionInfo;
    }

    @Override
    @SuppressWarnings("all")
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof InstallProfile)) return false;
        final InstallProfile other = (InstallProfile) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$installData = this.getInstallData();
        final Object other$installData = other.getInstallData();
        if (this$installData == null ? other$installData != null : !this$installData.equals(other$installData)) return false;
        final Object this$versionInfo = this.getVersionInfo();
        final Object other$versionInfo = other.getVersionInfo();
        if (this$versionInfo == null ? other$versionInfo != null : !this$versionInfo.equals(other$versionInfo)) return false;
        return true;
    }

    @SuppressWarnings("all")
    protected boolean canEqual(final Object other) {
        return other instanceof InstallProfile;
    }

    @Override
    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $installData = this.getInstallData();
        result = result * PRIME + ($installData == null ? 43 : $installData.hashCode());
        final Object $versionInfo = this.getVersionInfo();
        result = result * PRIME + ($versionInfo == null ? 43 : $versionInfo.hashCode());
        return result;
    }

    @Override
    @SuppressWarnings("all")
    public String toString() {
        return "InstallProfile(installData=" + this.getInstallData() + ", versionInfo=" + this.getVersionInfo() + ")";
    }
}