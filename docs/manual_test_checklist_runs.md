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
