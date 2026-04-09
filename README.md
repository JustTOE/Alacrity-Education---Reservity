# Reservity (Web App)

## Overview
Reservity is a platform designed to bridge the gap between organizations with available space and individuals or groups needing a place to work or host events. 

NGOs, local businesses, and community associations can use this platform to list their unused spaces, such as open-access STEAM labs, meeting rooms, or co-working desks. Spaces can be offered entirely for free or rented out for a fee. The platform provides a streamlined reservation system where guests can request time slots and hosts can review and approve these requests to ensure a good fit for their community guidelines.

## Core Features
* **Host Administration:** Secure admin accounts for NGOs and businesses to manage their organizational profiles.
* **Dynamic Space Configuration:** Hosts can create multiple space listings within their location (e.g., a 3D printing station, a conference room, a working desk).
* **Customizable Parameters:** For each space, hosts can configure:
    * Operating hours and available time slots.
    * Maximum capacity.
    * Specific requirements or rules for use.
    * Pricing (can be set to "Free/0" or a specific hourly/daily rate).
* **User/Guest Portal:** Individuals can create accounts to browse available spaces in their area.
* **Booking & Request System:** Users can select a space, specify their needs, and request a duration. 
* **Approval Workflow:** Hosts receive pending requests and have full authority to accept or deny them based on the provided user details.

## Tech Stack
* **Frontend:** SvelteKit ( + Svelte), TypeScrip
* **Backend:** SpringBoot
* **Database:** PostgreSQL, Supabase

## Figma Mock-ups
[See here:](https://www.figma.com/make/ROPDahrK5IGy7VjMjpXkzg/Public-Space-Rental-Website?p=f&fullscreen=1)

---

## Agile User Stories

The following user stories have been drafted using the formal format recommended by [Agile Modeling](https://agilemodeling.com/artifacts/userStory.htm) (`As a [role] I want [something] so that [benefit]`). They also include elements typically found on physical story cards, such as priority, size estimates (in story points), and Confirmations (Acceptance Criteria) usually written on the back of the card.

### User Story 1: Configuring a Space
**Story ID:** 101
**Priority:** 1 (Must Have)
**Estimate:** 5 Story Points

**Front of Card (The Story):**
> As an NGO Admin, I want to configure the details of an available space (such as time, requirements, max capacity, and price) so that users know exactly what is available and the rules for using it.

**Back of Card (Confirmations / Acceptance Criteria):**
* *Confirm that* the admin can input a title and description for the space (e.g., "STEAM Lab Main Room").
* *Confirm that* the admin can set a maximum occupancy limit for the space.
* *Confirm that* the admin can define operating hours and block out unavailable time slots.
* *Confirm that* the admin can toggle the space as "Free" or assign a specific monetary price.

---

### User Story 2: Requesting a Reservation
**Story ID:** 102
**Priority:** 1 (Must Have)
**Estimate:** 3 Story Points

**Front of Card (The Story):**
> As a registered user, I want to submit a reservation request for a specific room and duration so that I can secure a place to work or organize my event.

**Back of Card (Confirmations / Acceptance Criteria):**
* *Confirm that* the user can select a date, start time, and end time from the space's available schedule.
* *Confirm that* the user is presented with a text field to detail what they need the space for.
* *Confirm that* upon submission, the user's dashboard shows the request status as "Pending".
* *Confirm that* the system prevents the user from requesting a reservation if the space is already fully booked for that time.

---

### User Story 3: Managing Reservations
**Story ID:** 103
**Priority:** 2 (Should Have)
**Estimate:** 3 Story Points

**Front of Card (The Story):**
> As an NGO Admin, I want to review, accept, or deny pending reservation requests so that I can control who accesses our spaces and ensure our specific requirements are being met.

**Back of Card (Confirmations / Acceptance Criteria):**
* *Confirm that* the admin can view a list of all "Pending" requests within their host dashboard.
* *Confirm that* the admin can click on a request to read the user's specific needs and notes.
* *Confirm that* clicking "Accept" changes the request status to "Approved" and locks the time slot.
* *Confirm that* clicking "Deny" changes the request status to "Denied" and keeps the time slot open for others.
* *Confirm that* the user receives an automated notification regarding the host's final decision.
