
drop table commandes
drop table rapports
drop table produits
drop table clients
drop table typespro

create table clients (id identity not null primary key, nom varchar)
create table typespro (id identity not null primary key, lib varchar)
create table produits (id identity not null primary key, lib varchar, prix real,
                        idtyp bigint not null references typespro(id))
create table rapports (id identity not null primary key, montant real)
                        idpro bigint not null references produits(id), 
create table commandes (idpro bigint not null references produits(id), 
                        idcli bigint not null references clients(id),
                        datecmd datetime not null)
