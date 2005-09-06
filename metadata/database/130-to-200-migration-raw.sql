-- User permissions within a website
create table roller_user_permissions (
    id              varchar(48) not null primary key,
    website_id      varchar(48) not null,
    user_id         varchar(48) not null,
    permission_mask integer not null, -- bitmask 001 limited, 011 author, 100 admin
    pending         @BOOLEAN_SQL_TYPE_TRUE@ not null -- pending user acceptance of invitation to join website
);

-- Audit log records time and comment about change
create table roller_audit_log (
    id              varchar(48) not null primary key,
    user_id         varchar(48) not null,  -- user that made change
    object_id       varchar(48),           -- id of associated object, if any
    object_class    varchar(255),          -- name of associated object class (e.g. WeblogEntryData)
    comment         varchar(255) not null, -- description of change
    change_time     timestamp             -- time that change was made
);

-- Add new handle field to uniquely identify websites in URLs
alter table website add column handle varchar(255) @ALTER_TABLE_NOT_NULL@;
alter table website add column datecreated  timestamp @ALTER_TABLE_NOT_NULL@;
create index website_handle_index on website(handle);
alter table website add constraint website_handle_uq unique (handle@INDEXSIZE@);

-- Add userid to weblogentry so we can track original creator of entry
alter table weblogentry add column userid varchar(48) @ALTER_TABLE_NOT_NULL@;
alter table weblogentry add column status varchar(20) @ALTER_TABLE_NOT_NULL@;
create index weblogentry_userid_index on weblogentry(userid);

alter table rolleruser add column isenabled @BOOLEAN_SQL_TYPE_TRUE@ @ALTER_TABLE_NOT_NULL@;
alter table rolleruser add column locale varchar(50) @ALTER_TABLE_NOT_NULL@;
alter table rolleruser add column timezone varchar(50) @ALTER_TABLE_NOT_NULL@;
create index user_isenabled_index on rolleruser( isenabled );

-- -----------------------------------------------------
-- For ROL-754. MySQL 5.x introduced a new keyword "condition"
-- which made the use of "condition" as a column name in the "pingtarget" table illegal.
-- This renames the column to "conditioncode".   There is a corresponding change in the
-- Hibernate mapping metadata.

-- Create the new column.  If your database will not autopopulate new columns with default values, you may
-- have to remove the "not null" clause here.
alter table pingtarget add column conditioncode integer default 0 not null;

-- Transfer old column data to the new column.  This is not critical as currently it is not used, and
-- later the data will be generated by usage in the ping processor.
update pingtarget pt set pt.conditioncode=pt.condition;

-- Drop the old column.
alter table pingtarget drop column condition;
-- -----------------------------------------------------
