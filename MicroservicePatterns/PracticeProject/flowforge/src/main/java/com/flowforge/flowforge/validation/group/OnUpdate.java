package com.flowforge.flowforge.validation.group;

import jakarta.validation.groups.Default;

// Marker interface for validation group: rules that apply only on UPDATE
// Extends Default so that all non-grouped annotations (@Size, @Min, etc.) also run
public interface OnUpdate extends Default {
}
