
-- ============================================================================
-- SMART SOCIETY CONNECT - COMPLETE DATABASE SETUP (MySQL 8+)
-- ============================================================================
-- Purpose:
--   This is the ONE authoritative SQL file for a brand-new installation.
--   It includes Authentication, Residents, Visitors, Complaints, Billing,
--   Razorpay, offline Cash/Cheque audit, Meetings, Notifications,
--   Announcements, Resident Documents, Society Documents and Staff Documents.
--
-- How to run from the MySQL client:
--   SOURCE C:/path/to/smart-society-connect/database/fresh_install_schema.sql;
--
-- Important:
--   1. Run this file once on a new MySQL server before starting Spring Boot.
--   2. Do not run the individual migration files after this fresh setup;
--      their final columns and tables are already included below.
--   3. For an existing older database, use only the required migration file.
--   4. The first registered application user becomes the approved ADMIN after
--      OTP verification; later registrations require administrator approval.
-- ============================================================================
CREATE DATABASE IF NOT EXISTS smart_society_connect_db
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_society_connect_db;

-- ---------------------------------------------------------------------------
-- ANNOUNCEMENTS
-- ---------------------------------------------------------------------------
-- Public and audience-targeted notices published by Admin or Secretary.
CREATE TABLE IF NOT EXISTS announcements (
  id INT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(150) NOT NULL,
  message TEXT NOT NULL,
  category VARCHAR(30) NOT NULL,
  priority VARCHAR(20) NOT NULL,
  audience VARCHAR(30) NOT NULL,
  publish_at DATETIME NOT NULL,
  expires_at DATETIME NULL,
  published TINYINT(1) NOT NULL DEFAULT 1,
  created_by INT NOT NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_announcement_publish_expiry (publish_at, expires_at),
  INDEX idx_announcement_audience (audience)
);

    -- -----------------------------------------------------------------------
    -- BILLING AND COLLECTIONS
    -- -----------------------------------------------------------------------
    create table billing (
        billing_month TINYINT UNSIGNED not null,
        billing_year SMALLINT UNSIGNED not null,
        due_date date not null,
        electricity_charge decimal(10,2) not null,
        maintenance_amount decimal(10,2) not null,
        other_charge decimal(10,2) not null,
        parking_charge decimal(10,2) not null,
        penalty decimal(10,2) not null,
        resident_id integer not null,
        total_amount decimal(10,2) not null,
        water_charge decimal(10,2) not null,
        created_at datetime(6) not null,
        updated_at datetime(6) not null,
        billing_id CHAR(36) not null,
        remarks varchar(255),
        status enum ('CANCELLED','OVERDUE','PAID','PARTIALLY_PAID','PENDING') not null,
        primary key (billing_id)
    ) engine=InnoDB;

    -- -----------------------------------------------------------------------
    -- COMPLAINT MANAGEMENT
    -- -----------------------------------------------------------------------
    create table complaints (
        complaint_id INT UNSIGNED not null auto_increment,
        resident_id integer not null,
        created_at datetime(6),
        resolved_at datetime(6),
        updated_at datetime(6),
        complaint_number varchar(20) not null,
        assigned_to varchar(100),
        title varchar(100) not null,
        attachment varchar(255),
        description TEXT not null,
        resolution_remarks TEXT,
        category ENUM('PLUMBING','ELECTRICAL','LIFT','PARKING','SECURITY','HOUSEKEEPING','WATER','GARDEN','OTHER') not null,
        priority ENUM('LOW','MEDIUM','HIGH','URGENT') DEFAULT 'MEDIUM' not null,
        status ENUM('OPEN','IN_PROGRESS','RESOLVED','CLOSED','REJECTED') DEFAULT 'OPEN' not null,
        primary key (complaint_id)
    ) engine=InnoDB;

    -- -----------------------------------------------------------------------
    -- RESIDENT DOCUMENT CATALOGUE AND VERIFICATION
    -- -----------------------------------------------------------------------
    create table documents (
        document_id integer not null auto_increment,
        file_size MEDIUMINT UNSIGNED,
        created_at datetime(6),
        mime_type varchar(50),
        document_name varchar(100) not null,
        file_path varchar(255) not null,
        document_type ENUM('AADHAAR','PAN','PROOF_OF_ADDRESS','OWNERSHIP','RENT_AGREEMENT','PROFILE_PHOTO','OTHER') not null,
        primary key (document_id)
    ) engine=InnoDB;

    -- -----------------------------------------------------------------------
    -- RESIDENTS, FLATS AND FAMILY MEMBERS
    -- -----------------------------------------------------------------------
    create table family_members (
        age tinyint,
        member_id integer not null auto_increment,
        resident_id integer not null,
        created_at datetime(6),
        member_name varchar(100) not null,
        mobile CHAR(10),
        relation ENUM('FATHER','MOTHER','SPOUSE','SON','DAUGHTER','BROTHER','SISTER','OTHER') not null,
        primary key (member_id)
    ) engine=InnoDB;

    create table flats (
        area_sqft SMALLINT UNSIGNED,
        flat_id SMALLINT UNSIGNED not null auto_increment,
        floor_number TINYINT UNSIGNED not null,
        created_at datetime(6),
        flat_number varchar(10) not null,
        flat_type ENUM('1RK','1BHK','2BHK','3BHK','4BHK') not null,
        wing CHAR(1) not null,
        status ENUM('VACANT','OCCUPIED') DEFAULT 'VACANT' not null,
        primary key (flat_id)
    ) engine=InnoDB;

    -- -----------------------------------------------------------------------
    -- MEETINGS, RSVP/ATTENDANCE AND MINUTES
    -- -----------------------------------------------------------------------
    create table meeting_attendees (
        attendee_id integer not null auto_increment,
        meeting_id integer not null,
        resident_id integer not null,
        created_at datetime(6),
        responded_at datetime(6),
        updated_at datetime(6),
        remarks varchar(255),
        attendance_status enum ('GOING','MAYBE','NOT_GOING','PENDING') not null,
        primary key (attendee_id)
    ) engine=InnoDB;

    create table meeting_minutes (
        meeting_id integer not null,
        minutes_id integer not null auto_increment,
        prepared_by integer not null,
        created_at datetime(6),
        updated_at datetime(6),
        summary TEXT not null,
        uploaded_file varchar(255),
        primary key (minutes_id)
    ) engine=InnoDB;

    create table meetings (
        created_by integer not null,
        end_time time(6) not null,
        meeting_date date not null,
        meeting_id integer not null auto_increment,
        start_time time(6) not null,
        created_at datetime(6),
        updated_at datetime(6),
        title varchar(100) not null,
        venue varchar(150) not null,
        meeting_link varchar(500),
        agenda TEXT,
        description TEXT,
        meeting_uuid CHAR(36) not null,
        audience enum ('ALL_RESIDENTS','COMMITTEE_ONLY') not null,
        meeting_mode enum ('HYBRID','IN_PERSON','ONLINE') not null,
        meeting_type enum ('COMMITTEE','EMERGENCY','GENERAL_BODY','OTHER','VENDOR_REVIEW') not null,
        status enum ('CANCELLED','COMPLETED','ONGOING','SCHEDULED') not null,
        primary key (meeting_id)
    ) engine=InnoDB;

    -- -----------------------------------------------------------------------
    -- AUTHENTICATION, OTP, USERS, ROLES AND SESSIONS
    -- -----------------------------------------------------------------------
    create table otp_verification (
        is_used TINYINT(1) DEFAULT 0 not null,
        otp_id integer not null auto_increment,
        user_id integer not null,
        created_at datetime(6),
        expiry_time datetime(6) not null,
        otp_code CHAR(6) not null,
        primary key (otp_id)
    ) engine=InnoDB;

    -- Payment history supports Razorpay plus office Cash/Cheque collection.
    -- Cheques are stored PENDING and later marked SUCCESS or FAILED.
    -- recorded_by_* provides an audit trail for office-entered payments.
    create table payment_history (
        amount_paid decimal(10,2) not null,
        created_at datetime(6) not null,
        payment_date datetime(6) not null,
        transaction_reference varchar(100),
        bank_name varchar(100),
        cheque_date date,
        billing_id CHAR(36) not null,
        payment_id CHAR(36) not null,
        remarks varchar(255),
        recorded_by_user_id integer,
        recorded_by_name varchar(100),
        payment_mode enum ('BANK_TRANSFER','CARD','CASH','CHEQUE','UPI') not null,
        payment_status enum ('FAILED','PENDING','SUCCESS') not null,
        primary key (payment_id)
    ) engine=InnoDB;

    create table resident_documents (
        document_id integer not null,
        resident_doc_id integer not null auto_increment,
        resident_id integer not null,
        uploaded_by integer not null,
        verified_by integer,
        created_at datetime(6),
        verified_at datetime(6),
        verification_status ENUM('PENDING','VERIFIED','REJECTED') DEFAULT 'PENDING' not null,
        primary key (resident_doc_id)
    ) engine=InnoDB;

    create table residents (
        flat_id SMALLINT UNSIGNED not null,
        is_primary_member TINYINT(1) DEFAULT 1 not null,
        move_in_date date not null,
        move_out_date date,
        resident_id integer not null auto_increment,
        user_id integer not null,
        created_at datetime(6),
        occupation varchar(100),
        emergency_contact CHAR(10),
        resident_type ENUM('OWNER','TENANT') not null,
        primary key (resident_id)
    ) engine=InnoDB;

    create table roles (
        role_id tinyint not null auto_increment,
        role_name varchar(20) not null,
        role_description varchar(100),
        primary key (role_id)
    ) engine=InnoDB;

    -- -----------------------------------------------------------------------
    -- SOCIETY AND STAFF/SECURITY DOCUMENTS
    -- -----------------------------------------------------------------------
    create table society_documents (
        amount decimal(12,2),
        due_date date,
        paid_date date,
        published TINYINT(1) DEFAULT 0 not null,
        society_document_id integer not null auto_increment,
        uploaded_by integer not null,
        created_at datetime(6),
        billing_month varchar(20),
        category varchar(40) not null,
        vendor varchar(120),
        title varchar(150) not null,
        file_path varchar(255) not null,
        primary key (society_document_id)
    ) engine=InnoDB;

    create table staff_documents (
        expiry_date date,
        staff_document_id integer not null auto_increment,
        staff_user_id integer not null,
        uploaded_by integer not null,
        created_at datetime(6),
        verification_status varchar(20) not null,
        document_type varchar(40) not null,
        document_name varchar(150) not null,
        file_path varchar(255) not null,
        primary key (staff_document_id)
    ) engine=InnoDB;

    -- -----------------------------------------------------------------------
    -- USER-SPECIFIC NOTIFICATIONS
    -- -----------------------------------------------------------------------
    create table user_notifications (
        is_read TINYINT(1) DEFAULT 0 not null,
        notification_id integer not null auto_increment,
        recipient_id integer not null,
        created_at datetime(6),
        type varchar(40) not null,
        title varchar(140) not null,
        target_path varchar(160),
        message varchar(1000) not null,
        primary key (notification_id)
    ) engine=InnoDB;

    create table user_roles (
        role_id tinyint not null,
        user_id integer not null,
        primary key (role_id, user_id)
    ) engine=InnoDB;

    create table user_sessions (
        is_logged_out TINYINT(1) DEFAULT 0 not null,
        session_id integer not null auto_increment,
        user_id integer not null,
        login_time datetime(6),
        logout_time datetime(6),
        ip_address varchar(45),
        device_info varchar(150),
        jwt_token varchar(500) not null,
        primary key (session_id)
    ) engine=InnoDB;

    create table users (
        is_active TINYINT(1) DEFAULT 1 not null,
        is_verified TINYINT(1) DEFAULT 0 not null,
        user_id integer not null auto_increment,
        created_at datetime(6),
        updated_at datetime(6),
        approval_status varchar(20) not null,
        first_name varchar(30) not null,
        last_name varchar(30),
        email varchar(100) not null,
        mobile CHAR(10) not null,
        password_hash varchar(255) not null,
        profile_image varchar(255),
        primary key (user_id)
    ) engine=InnoDB;

    -- -----------------------------------------------------------------------
    -- VISITOR REQUESTS, APPROVALS AND SECURITY CHECK-IN
    -- -----------------------------------------------------------------------
    create table visitors (
        age TINYINT UNSIGNED not null,
        approved_by integer,
        created_by integer not null,
        expected_time time(6),
        resident_id integer not null,
        visit_date date not null,
        visitor_id INT UNSIGNED not null auto_increment,
        check_in_time datetime(6),
        check_out_time datetime(6),
        created_at datetime(6),
        updated_at datetime(6),
        vehicle_number varchar(20),
        id_proof_number varchar(30) not null,
        visitor_name varchar(100) not null,
        address varchar(255) not null,
        mobile CHAR(10) not null,
        photo_url varchar(255),
        purpose varchar(255) not null,
        remarks varchar(255),
        gender ENUM('MALE','FEMALE','OTHER') not null,
        id_proof_type ENUM('AADHAAR','PAN','PASSPORT','DRIVING_LICENSE','VOTER_ID','OTHER') not null,
        status ENUM('REQUESTED','APPROVED','REJECTED','CHECKED_IN','CHECKED_OUT') DEFAULT 'REQUESTED',
        primary key (visitor_id)
    ) engine=InnoDB;

    create index idx_billing_resident 
       on billing (resident_id);

    create index idx_billing_status 
       on billing (status);

    create index idx_billing_month_year 
       on billing (billing_month, billing_year);

    create index idx_billing_due_date 
       on billing (due_date);

    alter table billing 
       add constraint uk_resident_month_year unique (resident_id, billing_month, billing_year);

    create index idx_complaint_resident 
       on complaints (resident_id);

    create index idx_complaint_status 
       on complaints (status);

    create index idx_complaint_priority 
       on complaints (priority);

    create index idx_complaint_category 
       on complaints (category);

    create index idx_complaint_created 
       on complaints (created_at);

    alter table complaints 
       add constraint UK348qxg8v14l5t9lp4a9sxxyt6 unique (complaint_number);

    create index idx_document_type 
       on documents (document_type);

    create index idx_family_resident_id 
       on family_members (resident_id);

    alter table flats 
       add constraint UK8pw827lo1gcmv6ajimliexkds unique (flat_number);

    alter table meeting_attendees 
       add constraint uk_meeting_resident unique (meeting_id, resident_id);

    alter table meeting_minutes 
       add constraint UKm32oav9yfdhnxfhaqn5e7l59 unique (meeting_id);

    alter table meetings 
       add constraint UKbhnrmjqkyjjm50s90ipx5e0sw unique (meeting_uuid);

    create index idx_otp_user_id 
       on otp_verification (user_id);

    create index idx_payment_billing 
       on payment_history (billing_id);

    create index idx_payment_status 
       on payment_history (payment_status);

    create index idx_payment_mode 
       on payment_history (payment_mode);

    create index idx_payment_date 
       on payment_history (payment_date);

    create index idx_payment_recorded_by
       on payment_history (recorded_by_user_id);

    alter table payment_history 
       add constraint UKojyp82x5kww7gi6ocsqa64ipp unique (transaction_reference);

    create index idx_resident_doc_resident 
       on resident_documents (resident_id);

    create index idx_resident_doc_document 
       on resident_documents (document_id);

    create index idx_resident_doc_status 
       on resident_documents (verification_status);

    create index idx_resident_flat_id 
       on residents (flat_id);

    alter table residents 
       add constraint UKjsbnl61x7x3uqnnwd26akee7x unique (user_id);

    alter table roles 
       add constraint UK716hgxp60ym1lifrdgp67xt5k unique (role_name);

    create index idx_notification_recipient_read 
       on user_notifications (recipient_id, is_read);

    alter table users 
       add constraint UK6dotkott2kjsp8vw4d0m25fb7 unique (email);

    alter table users 
       add constraint UK63cf888pmqtt5tipcne79xsbm unique (mobile);

    create index idx_visitor_mobile 
       on visitors (mobile);

    create index idx_visitor_resident 
       on visitors (resident_id);

    create index idx_visitor_status 
       on visitors (status);

    create index idx_visitor_visit_date 
       on visitors (visit_date);

    create index idx_visitor_checkin 
       on visitors (check_in_time);

    alter table billing 
       add constraint FKqhoeqh3jp7wskjqejybptbpoi 
       foreign key (resident_id) 
       references residents (resident_id);

    alter table complaints 
       add constraint FKf3afx3em23uttfhtjobik1cg 
       foreign key (resident_id) 
       references residents (resident_id);

    alter table family_members 
       add constraint FKb0mkr4spg9a3u1nqahbccn8xg 
       foreign key (resident_id) 
       references residents (resident_id);

    alter table meeting_attendees 
       add constraint FKjrrh506w4qj7m1kap6fup86ni 
       foreign key (meeting_id) 
       references meetings (meeting_id);

    alter table meeting_attendees 
       add constraint FKtjcf6tdv4a8in4not2a8v06gw 
       foreign key (resident_id) 
       references residents (resident_id);

    alter table meeting_minutes 
       add constraint FK2kaic07c9fhvg2r6u5asq8vr8 
       foreign key (meeting_id) 
       references meetings (meeting_id);

    alter table meeting_minutes 
       add constraint FKpk5gffkgpms6rf3a3rue6n9ua 
       foreign key (prepared_by) 
       references users (user_id);

    alter table meetings 
       add constraint FK91iiqwhms7obbc92so3wxqq6f 
       foreign key (created_by) 
       references users (user_id);

    alter table otp_verification 
       add constraint FKmtitrif16hpdkhtr4m4kgvfv8 
       foreign key (user_id) 
       references users (user_id);

    alter table payment_history 
       add constraint FK4gr711olbk5d8qnpjd0e7dl58 
       foreign key (billing_id) 
       references billing (billing_id);

    alter table resident_documents 
       add constraint FKsajptfxsfvar3soeuwlul84fd 
       foreign key (document_id) 
       references documents (document_id);

    alter table resident_documents 
       add constraint FKaqjjr9ia23aofjpyrjss75w9j 
       foreign key (resident_id) 
       references residents (resident_id);

    alter table resident_documents 
       add constraint FKi7xcjowcfl8fmdqdwmtoy9lcn 
       foreign key (uploaded_by) 
       references users (user_id);

    alter table resident_documents 
       add constraint FKmtlesmg3g6u611iytm1ae46y9 
       foreign key (verified_by) 
       references users (user_id);

    alter table residents 
       add constraint FKeqba6k0sv1mnghteccis3okf3 
       foreign key (flat_id) 
       references flats (flat_id);

    alter table residents 
       add constraint FKg38luqcgm3qqs0s9lgjdfl9ov 
       foreign key (user_id) 
       references users (user_id);

    alter table society_documents 
       add constraint FKfprmnf3wj6dlldhck08qh304v 
       foreign key (uploaded_by) 
       references users (user_id);

    alter table staff_documents 
       add constraint FKljvlqdqoyi6iu7mrdts1sm5cv 
       foreign key (staff_user_id) 
       references users (user_id);

    alter table staff_documents 
       add constraint FK4dd7c0tm9xkhbeitcc9cuspvr 
       foreign key (uploaded_by) 
       references users (user_id);

    alter table user_notifications 
       add constraint FK7hgy29x2l8e5t2f80opi2ir5b 
       foreign key (recipient_id) 
       references users (user_id);

    alter table user_roles 
       add constraint FKh8ciramu9cc9q3qcqiv4ue8a6 
       foreign key (role_id) 
       references roles (role_id);

    alter table user_roles 
       add constraint FKhfh9dx7w3ubf1co1vdev94g3f 
       foreign key (user_id) 
       references users (user_id);

    alter table user_sessions 
       add constraint FK8klxsgb8dcjjklmqebqp1twd5 
       foreign key (user_id) 
       references users (user_id);

    alter table visitors 
       add constraint FKluyq9293ry9tfo4fqgffrihwa 
       foreign key (approved_by) 
       references users (user_id);

    alter table visitors 
       add constraint FKfsfhgefg0a62im1dawevdxugu 
       foreign key (created_by) 
       references users (user_id);

    alter table visitors 
       add constraint FKgj299rxaeu7t58ss0sbghyx1b 
       foreign key (resident_id) 
       references residents (resident_id);

-- Razorpay order attempts are kept separately from successful payment history.
-- This allows retries without marking a bill paid before gateway verification.
-- ---------------------------------------------------------------------------
-- RAZORPAY ORDER TRACKING
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payment_gateway_orders (
    gateway_order_record_id CHAR(36) NOT NULL,
    billing_id CHAR(36) NOT NULL,
    razorpay_order_id VARCHAR(100) NOT NULL,
    razorpay_payment_id VARCHAR(100) NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (gateway_order_record_id),
    CONSTRAINT uk_gateway_razorpay_order UNIQUE (razorpay_order_id),
    CONSTRAINT uk_gateway_razorpay_payment UNIQUE (razorpay_payment_id),
    CONSTRAINT fk_gateway_order_billing
        FOREIGN KEY (billing_id) REFERENCES billing (billing_id),
    INDEX idx_gateway_order_billing (billing_id),
    INDEX idx_gateway_order_status (status)
) ENGINE=InnoDB;

-- Add this relationship after both announcements and users exist.
ALTER TABLE announcements
  ADD CONSTRAINT fk_announcement_creator
  FOREIGN KEY (created_by) REFERENCES users(user_id);

-- Required application roles. The first account registered through the app is
-- automatically given ADMIN and APPROVED status (it still verifies its OTP).
INSERT INTO roles (role_name, role_description) VALUES
  ('ADMIN', 'System Administrator'),
  ('SECRETARY', 'Society Secretary'),
  ('RESIDENT', 'Society Resident'),
  ('SECURITY', 'Security Guard'),
  ('VISITOR', 'Temporary Visitor'),
  ('ACCOUNTANT', 'Society Accountant');

