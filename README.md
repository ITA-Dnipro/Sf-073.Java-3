# **Demo project for simple ORM (Object-Relational Mapping) Manager.**

The ORM manager works with autogenerated at DB side Long or Integer ID's. 
As of now, can work with these databases: H2.

#### The ORM Manager can work with generic entities under these constraints:
* every entity class must have an @Entity annotation;
* every entity class must have a field (Long or Integer) with @Id annotation.

#### Methods that the ORM Manager provides:
* _register_ - providing an entity class or multiple ones, creates table(s) in DB based on the entity's properties;
* _save_ - persists an entity. It assigns an identifier if entity doesn't exist in the DB.
If ID is present, save method performs an update. In both cases the method returns the saved/updated entity;
* _persist_ - it is intended for a first save of a new entity to DB.
It assigns an identifier if entity doesn't exist in the DB;
* _update_ - updates the existing object, and updates its row in the DB table.
If the object's identifier does not exist, it throws an exception;
* _findById_ - providing an ID and a class, returns the current object if exists or empty optional,
from the correct table, based on the provided class.
* _findAll_ - providing a class, returns a collection of all the objects, from the table, based on the provided class;
* _update_ - updates an existing object and returns it.
If the identifier of the object does not exist, it throws an exception.
* _refresh_ - synchronizing the provided object with its corresponding row in the DB table
and re-populate the object with the latest data available in database;
* _delete_ - can work with one or multiple objects. Returns true or false if the provided object is 
successfully deleted from the DB and sets the autogenerated ID of the object to null if it was deleted from the DB side.
If the object is not present in the DB, the method returns false;
* _recordsCount_ - returns the number of all records from the table, based on the provided class.

###### ORM Manager works with the current annotations: @Entity, @Table, @Id, @Column, @ManyToOne, @OneToMany.