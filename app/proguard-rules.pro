# Glance pulls androidx.work transitively; WorkManager 2.7.1 instantiates its
# Room database by reflecting on the generated WorkDatabase_Impl class name.
# R8 full mode renames that class (nothing references it directly), so the
# startup initializer crashes before any activity shows. Keep the generated
# implementation and its name.
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
-keep class androidx.work.impl.WorkDatabase { <init>(); }
