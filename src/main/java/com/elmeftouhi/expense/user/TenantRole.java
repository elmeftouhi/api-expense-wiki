package com.elmeftouhi.expense.user;

public enum TenantRole {
    VIEWER,
    EDITOR,
    ADMIN;

    public boolean atLeast(TenantRole requiredRole) {
        return this.ordinal() >= requiredRole.ordinal();
    }
}

