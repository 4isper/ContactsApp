package com.m4isper.contactsapp.di

import com.m4isper.contactsapp.domain.repository.AccountSettings
import com.m4isper.contactsapp.domain.repository.AppearanceSettings
import com.m4isper.contactsapp.domain.repository.ContactsAccounts
import com.m4isper.contactsapp.domain.repository.ContactsMerger
import com.m4isper.contactsapp.domain.repository.ContactsReader
import com.m4isper.contactsapp.domain.repository.ContactsWriter
import com.m4isper.contactsapp.domain.repository.FavoritesRepository
import com.m4isper.contactsapp.domain.repository.GroupsRepository
import com.m4isper.contactsapp.domain.repository.LanguageSettings
import com.m4isper.contactsapp.domain.repository.ListSettings
import com.m4isper.contactsapp.domain.repository.PrivacySettings
import com.m4isper.contactsapp.domain.repository.ReminderSettings
import com.m4isper.contactsapp.domain.repository.SettingsMaintenance
import com.m4isper.contactsapp.domain.repository.SettingsReader
import com.m4isper.contactsapp.domain.security.PinHasher
import com.m4isper.contactsapp.domain.usecase.AddContactsToGroupUseCase
import com.m4isper.contactsapp.domain.usecase.CopyContactToAccountUseCase
import com.m4isper.contactsapp.domain.usecase.CreateContactUseCase
import com.m4isper.contactsapp.domain.usecase.CreateGroupUseCase
import com.m4isper.contactsapp.domain.usecase.DeleteContactsUseCase
import com.m4isper.contactsapp.domain.usecase.DeleteGroupUseCase
import com.m4isper.contactsapp.domain.usecase.ExportContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactAccountsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactDetailsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactFormUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactsPageUseCase
import com.m4isper.contactsapp.domain.usecase.GetContactsUseCase
import com.m4isper.contactsapp.domain.usecase.GetDefaultContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupMembershipUseCase
import com.m4isper.contactsapp.domain.usecase.GetGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.GetLastContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.GetProfileContactIdUseCase
import com.m4isper.contactsapp.domain.usecase.ImportContactsUseCase
import com.m4isper.contactsapp.domain.usecase.LoadContactPhotoUseCase
import com.m4isper.contactsapp.domain.usecase.MergeContactsUseCase
import com.m4isper.contactsapp.domain.usecase.MergeGroupsUseCase
import com.m4isper.contactsapp.domain.usecase.MoveContactToAccountUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.ObserveSettingsUseCase
import com.m4isper.contactsapp.domain.usecase.RefreshFavoritesUseCase
import com.m4isper.contactsapp.domain.usecase.RemoveContactsFromGroupUseCase
import com.m4isper.contactsapp.domain.usecase.RemoveFavoriteUseCase
import com.m4isper.contactsapp.domain.usecase.RenameGroupUseCase
import com.m4isper.contactsapp.domain.usecase.ResetSettingsUseCase
import com.m4isper.contactsapp.domain.usecase.SetAppLanguageUseCase
import com.m4isper.contactsapp.domain.usecase.SetAppLockUseCase
import com.m4isper.contactsapp.domain.usecase.SetContactPhotoUseCase
import com.m4isper.contactsapp.domain.usecase.SetDefaultContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.SetDynamicColorUseCase
import com.m4isper.contactsapp.domain.usecase.SetFavoritesFirstUseCase
import com.m4isper.contactsapp.domain.usecase.SetLastContactAccountUseCase
import com.m4isper.contactsapp.domain.usecase.SetListDensityUseCase
import com.m4isper.contactsapp.domain.usecase.SetRemindersUseCase
import com.m4isper.contactsapp.domain.usecase.SetSecureWindowUseCase
import com.m4isper.contactsapp.domain.usecase.SetShowPhotosUseCase
import com.m4isper.contactsapp.domain.usecase.SetSortOrderUseCase
import com.m4isper.contactsapp.domain.usecase.SetThemeModeUseCase
import com.m4isper.contactsapp.domain.usecase.ToggleFavoriteUseCase
import com.m4isper.contactsapp.domain.usecase.UnmergeContactsUseCase
import com.m4isper.contactsapp.domain.usecase.UpdateContactUseCase
import com.m4isper.contactsapp.domain.vcard.VCardCodec
import dev.zacsweers.metro.Provides

// Подавление осознанное: это список сборки, а не логика - по одному методу на use case.
// Сборка use cases: их создаёт composition root, а не фреймворк внедрения
// зависимостей в самом домене. Конструкторы use cases не знают про DI.
@Suppress("TooManyFunctions")
interface UseCaseProviders {
    @Provides
    fun provideAddContactsToGroupUseCase(groupsRepository: GroupsRepository): AddContactsToGroupUseCase =
        AddContactsToGroupUseCase(
            groupsRepository,
        )

    @Provides
    fun provideCopyContactToAccountUseCase(
        contactsReader: ContactsReader,
        contactsWriter: ContactsWriter,
    ): CopyContactToAccountUseCase =
        CopyContactToAccountUseCase(
            contactsReader,
            contactsWriter,
        )

    @Provides
    fun provideCreateContactUseCase(contactsWriter: ContactsWriter): CreateContactUseCase =
        CreateContactUseCase(
            contactsWriter,
        )

    @Provides
    fun provideCreateGroupUseCase(groupsRepository: GroupsRepository): CreateGroupUseCase =
        CreateGroupUseCase(
            groupsRepository,
        )

    @Provides
    fun provideDeleteContactsUseCase(
        contactsWriter: ContactsWriter,
        favoritesRepository: FavoritesRepository,
    ): DeleteContactsUseCase =
        DeleteContactsUseCase(
            contactsWriter,
            favoritesRepository,
        )

    @Provides
    fun provideDeleteGroupUseCase(groupsRepository: GroupsRepository): DeleteGroupUseCase =
        DeleteGroupUseCase(
            groupsRepository,
        )

    @Provides
    fun provideExportContactsUseCase(
        getContactDetailsUseCase: GetContactDetailsUseCase,
        loadContactPhotoUseCase: LoadContactPhotoUseCase,
        getGroupsUseCase: GetGroupsUseCase,
        getGroupMembershipUseCase: GetGroupMembershipUseCase,
        vCardCodec: VCardCodec,
    ): ExportContactsUseCase =
        ExportContactsUseCase(
            getContactDetailsUseCase,
            loadContactPhotoUseCase,
            getGroupsUseCase,
            getGroupMembershipUseCase,
            vCardCodec,
        )

    @Provides
    fun provideGetContactAccountUseCase(contactsAccounts: ContactsAccounts): GetContactAccountUseCase =
        GetContactAccountUseCase(
            contactsAccounts,
        )

    @Provides
    fun provideGetContactAccountsUseCase(contactsAccounts: ContactsAccounts): GetContactAccountsUseCase =
        GetContactAccountsUseCase(
            contactsAccounts,
        )

    @Provides
    fun provideGetContactDetailsUseCase(contactsReader: ContactsReader): GetContactDetailsUseCase =
        GetContactDetailsUseCase(
            contactsReader,
        )

    @Provides
    fun provideGetContactFormUseCase(contactsReader: ContactsReader): GetContactFormUseCase =
        GetContactFormUseCase(
            contactsReader,
        )

    @Provides
    fun provideGetContactGroupsUseCase(groupsRepository: GroupsRepository): GetContactGroupsUseCase =
        GetContactGroupsUseCase(
            groupsRepository,
        )

    @Provides
    fun provideGetContactsPageUseCase(contactsReader: ContactsReader): GetContactsPageUseCase =
        GetContactsPageUseCase(
            contactsReader,
        )

    @Provides
    fun provideGetContactsUseCase(contactsReader: ContactsReader): GetContactsUseCase =
        GetContactsUseCase(
            contactsReader,
        )

    @Provides
    fun provideGetDefaultContactAccountUseCase(settingsReader: SettingsReader): GetDefaultContactAccountUseCase =
        GetDefaultContactAccountUseCase(
            settingsReader,
        )

    @Provides
    fun provideGetGroupMembershipUseCase(groupsRepository: GroupsRepository): GetGroupMembershipUseCase =
        GetGroupMembershipUseCase(
            groupsRepository,
        )

    @Provides
    fun provideGetGroupsUseCase(groupsRepository: GroupsRepository): GetGroupsUseCase =
        GetGroupsUseCase(
            groupsRepository,
        )

    @Provides
    fun provideGetLastContactAccountUseCase(settingsReader: SettingsReader): GetLastContactAccountUseCase =
        GetLastContactAccountUseCase(
            settingsReader,
        )

    @Provides
    fun provideGetProfileContactIdUseCase(contactsReader: ContactsReader): GetProfileContactIdUseCase =
        GetProfileContactIdUseCase(
            contactsReader,
        )

    @Provides
    fun provideImportContactsUseCase(
        createContactUseCase: CreateContactUseCase,
        mergeContactsUseCase: MergeContactsUseCase,
        setContactPhotoUseCase: SetContactPhotoUseCase,
        addContactsToGroupUseCase: AddContactsToGroupUseCase,
        getGroupsUseCase: GetGroupsUseCase,
        deleteContactsUseCase: DeleteContactsUseCase,
    ): ImportContactsUseCase =
        ImportContactsUseCase(
            createContactUseCase,
            mergeContactsUseCase,
            setContactPhotoUseCase,
            addContactsToGroupUseCase,
            getGroupsUseCase,
            deleteContactsUseCase,
        )

    @Provides
    fun provideLoadContactPhotoUseCase(contactsReader: ContactsReader): LoadContactPhotoUseCase =
        LoadContactPhotoUseCase(
            contactsReader,
        )

    @Provides
    fun provideMergeContactsUseCase(contactsMerger: ContactsMerger): MergeContactsUseCase =
        MergeContactsUseCase(
            contactsMerger,
        )

    @Provides
    fun provideMergeGroupsUseCase(groupsRepository: GroupsRepository): MergeGroupsUseCase =
        MergeGroupsUseCase(
            groupsRepository,
        )

    @Provides
    fun provideMoveContactToAccountUseCase(
        copyContactToAccountUseCase: CopyContactToAccountUseCase,
        contactsWriter: ContactsWriter,
    ): MoveContactToAccountUseCase =
        MoveContactToAccountUseCase(
            copyContactToAccountUseCase,
            contactsWriter,
        )

    @Provides
    fun provideObserveFavoritesUseCase(favoritesRepository: FavoritesRepository): ObserveFavoritesUseCase =
        ObserveFavoritesUseCase(
            favoritesRepository,
        )

    @Provides
    fun provideObserveSettingsUseCase(settingsReader: SettingsReader): ObserveSettingsUseCase =
        ObserveSettingsUseCase(
            settingsReader,
        )

    @Provides
    fun provideRefreshFavoritesUseCase(favoritesRepository: FavoritesRepository): RefreshFavoritesUseCase =
        RefreshFavoritesUseCase(
            favoritesRepository,
        )

    @Provides
    fun provideRemoveContactsFromGroupUseCase(groupsRepository: GroupsRepository): RemoveContactsFromGroupUseCase =
        RemoveContactsFromGroupUseCase(
            groupsRepository,
        )

    @Provides
    fun provideRemoveFavoriteUseCase(favoritesRepository: FavoritesRepository): RemoveFavoriteUseCase =
        RemoveFavoriteUseCase(
            favoritesRepository,
        )

    @Provides
    fun provideRenameGroupUseCase(groupsRepository: GroupsRepository): RenameGroupUseCase =
        RenameGroupUseCase(
            groupsRepository,
        )

    @Provides
    fun provideResetSettingsUseCase(settingsMaintenance: SettingsMaintenance): ResetSettingsUseCase =
        ResetSettingsUseCase(
            settingsMaintenance,
        )

    @Provides
    fun provideSetAppLanguageUseCase(languageSettings: LanguageSettings): SetAppLanguageUseCase =
        SetAppLanguageUseCase(
            languageSettings,
        )

    @Provides
    fun provideSetAppLockUseCase(
        privacySettings: PrivacySettings,
        pinHasher: PinHasher,
    ): SetAppLockUseCase =
        SetAppLockUseCase(
            privacySettings,
            pinHasher,
        )

    @Provides
    fun provideSetContactPhotoUseCase(contactsWriter: ContactsWriter): SetContactPhotoUseCase =
        SetContactPhotoUseCase(
            contactsWriter,
        )

    @Provides
    fun provideSetDefaultContactAccountUseCase(accountSettings: AccountSettings): SetDefaultContactAccountUseCase =
        SetDefaultContactAccountUseCase(
            accountSettings,
        )

    @Provides
    fun provideSetDynamicColorUseCase(appearanceSettings: AppearanceSettings): SetDynamicColorUseCase =
        SetDynamicColorUseCase(
            appearanceSettings,
        )

    @Provides
    fun provideSetFavoritesFirstUseCase(listSettings: ListSettings): SetFavoritesFirstUseCase =
        SetFavoritesFirstUseCase(
            listSettings,
        )

    @Provides
    fun provideSetLastContactAccountUseCase(accountSettings: AccountSettings): SetLastContactAccountUseCase =
        SetLastContactAccountUseCase(
            accountSettings,
        )

    @Provides
    fun provideSetListDensityUseCase(listSettings: ListSettings): SetListDensityUseCase =
        SetListDensityUseCase(
            listSettings,
        )

    @Provides
    fun provideSetRemindersUseCase(reminderSettings: ReminderSettings): SetRemindersUseCase =
        SetRemindersUseCase(
            reminderSettings,
        )

    @Provides
    fun provideSetSecureWindowUseCase(privacySettings: PrivacySettings): SetSecureWindowUseCase =
        SetSecureWindowUseCase(
            privacySettings,
        )

    @Provides
    fun provideSetShowPhotosUseCase(appearanceSettings: AppearanceSettings): SetShowPhotosUseCase =
        SetShowPhotosUseCase(
            appearanceSettings,
        )

    @Provides
    fun provideSetSortOrderUseCase(listSettings: ListSettings): SetSortOrderUseCase =
        SetSortOrderUseCase(
            listSettings,
        )

    @Provides
    fun provideSetThemeModeUseCase(appearanceSettings: AppearanceSettings): SetThemeModeUseCase =
        SetThemeModeUseCase(
            appearanceSettings,
        )

    @Provides
    fun provideToggleFavoriteUseCase(favoritesRepository: FavoritesRepository): ToggleFavoriteUseCase =
        ToggleFavoriteUseCase(
            favoritesRepository,
        )

    @Provides
    fun provideUnmergeContactsUseCase(contactsMerger: ContactsMerger): UnmergeContactsUseCase =
        UnmergeContactsUseCase(
            contactsMerger,
        )

    @Provides
    fun provideUpdateContactUseCase(contactsWriter: ContactsWriter): UpdateContactUseCase =
        UpdateContactUseCase(
            contactsWriter,
        )
}
