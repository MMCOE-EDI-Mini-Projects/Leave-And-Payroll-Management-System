create database login;

create table Employee(
    employee_id int(10) not null primary key,
    employee_code varchar(30) not null,
    first_name varchar(60) not null,
    last_name varchar(60) not null,
    date_of_birth date not null,
    gender varchar(20),
    email varchar(100),
    phone_no varchar(15),
    date_of_joining date not null,
    employee_rank varchar(40),
    created_at datetime not null,
    updated_at datetime not null,
)
