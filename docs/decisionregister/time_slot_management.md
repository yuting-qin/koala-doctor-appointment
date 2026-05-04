# How to manage time slots in the system

## Options
### Option 1: Dynamically calculate timeslot base on a doctor's schedule



### Option 2: Pre-generate time slots 
Use Schedule Template to generate time slots for doctors.  Clinic manager defines a doctor's
week(e.g., Mondays 9:00 AM – 5:00 PM with 15-minute intervals).
- Regular job(daily or weekly or on request) that looks at the template and generate rows in database representing each time slot
- Each row in `appintments` represents a specific block of time

### Pros
- Complex constraints
  - doctors work on different time for different clinics, 
  - doctors may only see new patient in the morning or follow ups in the afternoon(having visit types for slots)
  - some slots may allow multi booking, such as a group therapy
  - doctors may call in sick or take emergency leave, easy to mark those slots and unavailable 
- Buffer management
  - insert ghost slots for admin work/lunch breaks
- Multi resource locking 
  - A surgery might require doctor, nurse and a specific room, easier to query pre-generated slots where all resource intersect
- Query without complex calculating
  - Query for doctor availability are query the rows that are not booked/blocked, no need to calculate every time

### Cons
- Requires regular job to pre-generate appointment slots
- require storage for the slots that's never bookable(sick leave, holiday etc.)

## Decision
Prefers option 2

### Reasoning:



