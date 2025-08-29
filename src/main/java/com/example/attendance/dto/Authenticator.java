package com.example.attendance.dto;

import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.client.CollectedClientData;

public class Authenticator {

    // Application-specific user identifier
    private String userId;

    // Fields required/stored
    private byte[] credentialId;
    private AttestedCredentialData attestedCredentialData;
    private byte[] attestationObject;
    private CollectedClientData clientData;
    private long signCount;
    private boolean uvInitialized;
    private boolean backupEligible;
    private boolean backedUp;

    // Additional fields used by servlets/DAOs
    private byte[] publicKey;
    private byte[] aaguid;
    private String attestationType;
    private boolean backupState;

    // Getter and Setter for application-specific userId
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public byte[] getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(byte[] credentialId) {
        this.credentialId = credentialId;
    }

    public AttestedCredentialData getAttestedCredentialData() {
        return attestedCredentialData;
    }

    public void setAttestedCredentialData(AttestedCredentialData attestedCredentialData) {
        this.attestedCredentialData = attestedCredentialData;
    }

    public byte[] getAttestationObject() {
        return attestationObject;
    }

    public void setAttestationObject(byte[] attestationObject) {
        this.attestationObject = attestationObject;
    }

    public CollectedClientData getClientData() {
        return clientData;
    }

    public void setClientData(CollectedClientData clientData) {
        this.clientData = clientData;
    }

    public long getSignCount() {
        return signCount;
    }

    public void setSignCount(long signCount) {
        this.signCount = signCount;
    }

    public boolean isUvInitialized() {
        return uvInitialized;
    }

    public void setUvInitialized(boolean uvInitialized) {
        this.uvInitialized = uvInitialized;
    }

    public boolean isBackupEligible() {
        return backupEligible;
    }

    public void setBackupEligible(boolean backupEligible) {
        this.backupEligible = backupEligible;
    }

    public boolean isBackedUp() {
        return backedUp;
    }

    public void setBackedUp(boolean backedUp) {
        this.backedUp = backedUp;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public byte[] getAaguid() {
        return aaguid;
    }

    public void setAaguid(byte[] aaguid) {
        this.aaguid = aaguid;
    }

    public String getAttestationType() {
        return attestationType;
    }

    public void setAttestationType(String attestationType) {
        this.attestationType = attestationType;
    }

    public boolean isBackupState() {
        return backupState;
    }

    public void setBackupState(boolean backupState) {
        this.backupState = backupState;
    }
}
