erDiagram
    %% Core Entities & Memberships
    USER ||--o{ MEMBERSHIP : "participates in"
    ORGANIZATION ||--o{ MEMBERSHIP : "includes"
    
    %% Polymorphic Space Ownership
    USER ||--o{ SPACE : "owns (ownerType=USER)"
    ORGANIZATION ||--o{ SPACE : "owns (ownerType=ORGANIZATION)"
    
    %% Space Details & Booking Flow
    SPACE ||--o{ SPACE_IMAGE : "displays"
    SPACE ||--o{ RESERVATION_REQUEST : "receives"
    USER ||--o{ RESERVATION_REQUEST : "submits"
    
    %% Confirmed Reservations
    RESERVATION_REQUEST ||--o| RESERVATION : "generates (on APPROVAL)"
    SPACE ||--o{ RESERVATION : "blocks availability"
    
    %% Communication & Trust
    USER ||--o{ NOTIFICATION : "receives"
    USER ||--o{ REVIEW : "writes"
    
    %% Polymorphic Review Targets
    USER ||--o{ REVIEW : "receives (targetType=USER)"
    ORGANIZATION ||--o{ REVIEW : "receives (targetType=ORGANIZATION)"

    %% Entity Definitions
    USER {
        UUID userId PK
        String name
        String email
        String accountType "Enum: OWNER, REQUESTER, ADMIN"
    }

    ORGANIZATION {
        UUID orgId PK
        String name
        String profileDetails
    }

    MEMBERSHIP {
        UUID userId FK "PK/FK composite"
        UUID orgId FK "PK/FK composite"
        String role "Enum: ADMIN, MEMBER"
    }

    SPACE {
        UUID spaceId PK
        UUID ownerId FK "Polymorphic UUID"
        String ownerType "Enum: USER, ORGANIZATION"
        String title
        String description
        String category "Enum: APARTMENT, HALL, OFFICE"
        String operatingHours
        Int maxCapacity
        Double pricePerHour
        Double pricePerDay
        String currency
        Boolean isFree
        String city
        String address
        Double latitude
        Double longitude
        String rulesAndRequirements
    }

    SPACE_IMAGE {
        UUID id PK
        UUID spaceId FK
        String url
    }

    RESERVATION_REQUEST {
        UUID requestId PK
        UUID spaceId FK
        UUID userId FK "Requester"
        Timestamp requestedDate
        Timestamp startTime
        Timestamp endTime
        String purposeDescription
        String status "Enum: PENDING, APPROVED, DENIED"
        Timestamp createdAt
        Timestamp updatedAt
        Timestamp decisionDate
        String rejectionReason
        String contactPhone
        Int attendeesCount
    }

    RESERVATION {
        UUID id PK
        UUID spaceId FK
        UUID requestId FK "Source Request"
        Timestamp startTime
        Timestamp endTime
    }

    NOTIFICATION {
        UUID id PK
        UUID userId FK
        String type "Enum: EMAIL, IN_APP"
        String message
        Boolean read
        Timestamp createdAt
    }

    REVIEW {
        UUID id PK
        UUID reviewerId FK "Author"
        UUID targetId FK "Polymorphic Target UUID"
        String targetType "Enum: USER, ORGANIZATION"
        Int rating
        String comment
    }