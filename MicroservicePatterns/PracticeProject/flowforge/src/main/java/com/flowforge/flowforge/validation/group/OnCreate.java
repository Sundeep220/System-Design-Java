package com.flowforge.flowforge.validation.group;

import jakarta.validation.groups.Default;

// Marker interface for validation group: rules that apply only on CREATE
// Extends Default so that all non-grouped annotations (@Size, @Min, etc.) also run
public interface OnCreate extends Default {
}
