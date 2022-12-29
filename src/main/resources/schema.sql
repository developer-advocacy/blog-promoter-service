-- drop table spring_teammates cascade ;

create table if not exists spring_teammates
(

    url      text             not null,
    name     text primary key not null,
    position text             not null,
    location text             not null,
    github   text,
    twitter  text,
    fresh    bool             not null default true
);


create table if not exists blog_posts
(
    url        text primary key not null,
    published  timestamp        not null,
    categories text[],
    title      text             not null,
    blog_id    text             not null,
    author     text             null,
    promoted   timestamp        null

);

create table if not exists spring_integration_metadata_store
(
    key   text not null unique primary key,
    value text not null
);