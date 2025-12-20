# Manual Test Checklist - Runs Management

## 1. List Runs
- [ ] Open `/runs` page
- [ ] Verify runs list is loaded
- [ ] Verify pagination works (change size, page)
- [ ] Verify filtering by Project, Status, Search works
- [ ] Verify Reset button clears filters

## 2. Create Run
- [ ] Click "Create Run" button
- [ ] Fill required fields (Project, Name, Status)
- [ ] Add Metrics:
    - [ ] Click "Add Metric"
    - [ ] Select Metric Definition
    - [ ] Enter Value
    - [ ] Add another metric
    - [ ] Verify duplicate metric selection is disabled/warned
- [ ] Click "Confirm"
- [ ] Verify new run appears in list
- [ ] Verify success message

## 3. Detail Drawer
- [ ] Click on a run row
- [ ] Verify Drawer opens
- [ ] Verify basic info is correct
- [ ] Verify Tags are displayed
- [ ] Verify Metrics table shows correct values

## 4. Edit Run
- [ ] Click "Edit" button on a row
- [ ] Verify Dialog opens with populated data
- [ ] Change Name and Status
- [ ] Add/Remove Metrics
- [ ] Click "Confirm"
- [ ] Verify updates reflected in list/detail

## 5. Delete Run
- [ ] Click "Delete" button on a row
- [ ] Confirm warning dialog
- [ ] Verify run is removed from list
- [ ] Verify success message

---

## A2 Patch Manual Test Run (Run Dialog inline create)

Date: 2025-12-20

### Tag (P0)
- [ ] Open Run Create/Edit dialog, tags load without blocking
- [ ] Type a new tag, press Enter, tag is created and shown as a chip
- [ ] Type an existing tag, press Enter, no duplicate created and chip is added
- [ ] Remove a chip, tag is unselected (resource not deleted)
- [ ] Same tag cannot be added twice as chips

### MetricDef (P0)
- [ ] When metric defs are empty, select shows guidance and `+ New Metric` button
- [ ] Create MetricDef in popup, it appears in options and auto-selected on current row
- [ ] Same Run cannot select duplicate metric definition IDs
- [ ] Create/Update Run, detail drawer shows tags and metrics

### Regression (P0)
- [ ] Runs list, detail drawer, edit, delete still work
- [ ] Dashboard remains stable with empty/low data
