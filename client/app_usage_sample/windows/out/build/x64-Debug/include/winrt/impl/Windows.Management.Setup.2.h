// WARNING: Please don't edit this file. It was generated by C++/WinRT v2.0.220418.1

#pragma once
#ifndef WINRT_Windows_Management_Setup_2_H
#define WINRT_Windows_Management_Setup_2_H
#include "winrt/impl/Windows.Foundation.1.h"
#include "winrt/impl/Windows.Management.Setup.1.h"
WINRT_EXPORT namespace winrt::Windows::Management::Setup
{
    struct DeploymentSessionHeartbeatRequested : winrt::Windows::Foundation::IUnknown
    {
        DeploymentSessionHeartbeatRequested(std::nullptr_t = nullptr) noexcept {}
        DeploymentSessionHeartbeatRequested(void* ptr, take_ownership_from_abi_t) noexcept : winrt::Windows::Foundation::IUnknown(ptr, take_ownership_from_abi) {}
        template <typename L> DeploymentSessionHeartbeatRequested(L lambda);
        template <typename F> DeploymentSessionHeartbeatRequested(F* function);
        template <typename O, typename M> DeploymentSessionHeartbeatRequested(O* object, M method);
        template <typename O, typename M> DeploymentSessionHeartbeatRequested(com_ptr<O>&& object, M method);
        template <typename O, typename M> DeploymentSessionHeartbeatRequested(weak_ref<O>&& object, M method);
        auto operator()(winrt::Windows::Management::Setup::DeploymentSessionHeartbeatRequestedEventArgs const& eventArgs) const;
    };
    struct __declspec(empty_bases) AgentProvisioningProgressReport : winrt::Windows::Management::Setup::IAgentProvisioningProgressReport
    {
        AgentProvisioningProgressReport(std::nullptr_t) noexcept {}
        AgentProvisioningProgressReport(void* ptr, take_ownership_from_abi_t) noexcept : winrt::Windows::Management::Setup::IAgentProvisioningProgressReport(ptr, take_ownership_from_abi) {}
        AgentProvisioningProgressReport();
    };
    struct __declspec(empty_bases) DeploymentSessionConnectionChangedEventArgs : winrt::Windows::Management::Setup::IDeploymentSessionConnectionChangedEventArgs
    {
        DeploymentSessionConnectionChangedEventArgs(std::nullptr_t) noexcept {}
        DeploymentSessionConnectionChangedEventArgs(void* ptr, take_ownership_from_abi_t) noexcept : winrt::Windows::Management::Setup::IDeploymentSessionConnectionChangedEventArgs(ptr, take_ownership_from_abi) {}
    };
    struct __declspec(empty_bases) DeploymentSessionHeartbeatRequestedEventArgs : winrt::Windows::Management::Setup::IDeploymentSessionHeartbeatRequestedEventArgs
    {
        DeploymentSessionHeartbeatRequestedEventArgs(std::nullptr_t) noexcept {}
        DeploymentSessionHeartbeatRequestedEventArgs(void* ptr, take_ownership_from_abi_t) noexcept : winrt::Windows::Management::Setup::IDeploymentSessionHeartbeatRequestedEventArgs(ptr, take_ownership_from_abi) {}
    };
    struct __declspec(empty_bases) DeploymentSessionStateChangedEventArgs : winrt::Windows::Management::Setup::IDeploymentSessionStateChangedEventArgs
    {
        DeploymentSessionStateChangedEventArgs(std::nullptr_t) noexcept {}
        DeploymentSessionStateChangedEventArgs(void* ptr, take_ownership_from_abi_t) noexcept : winrt::Windows::Management::Setup::IDeploymentSessionStateChangedEventArgs(ptr, take_ownership_from_abi) {}
    };
    struct __declspec(empty_bases) DeploymentWorkload : winrt::Windows::Management::Setup::IDeploymentWorkload
    {
        DeploymentWorkload(std::nullptr_t) noexcept {}
        DeploymentWorkload(void* ptr, take_ownership_from_abi_t) noexcept : winrt::Windows::Management::Setup::IDeploymentWorkload(ptr, take_ownership_from_abi) {}
        explicit DeploymentWorkload(param::hstring const& id);
    };
    struct __declspec(empty_bases) DeploymentWorkloadBatch : winrt::Windows::Management::Setup::IDeploymentWorkloadBatch
    {
        DeploymentWorkloadBatch(std::nullptr_t) noexcept {}
        DeploymentWorkloadBatch(void* ptr, take_ownership_from_abi_t) noexcept : winrt::Windows::Management::Setup::IDeploymentWorkloadBatch(ptr, take_ownership_from_abi) {}
        explicit DeploymentWorkloadBatch(uint32_t id);
    };
    struct __declspec(empty_bases) DevicePreparationExecutionContext : winrt::Windows::Management::Setup::IDevicePreparationExecutionContext
    {
        DevicePreparationExecutionContext(std::nullptr_t) noexcept {}
        DevicePreparationExecutionContext(void* ptr, take_ownership_from_abi_t) noexcept : winrt::Windows::Management::Setup::IDevicePreparationExecutionContext(ptr, take_ownership_from_abi) {}
    };
    struct __declspec(empty_bases) MachineProvisioningProgressReporter : winrt::Windows::Management::Setup::IMachineProvisioningProgressReporter
    {
        MachineProvisioningProgressReporter(std::nullptr_t) noexcept {}
        MachineProvisioningProgressReporter(void* ptr, take_ownership_from_abi_t) noexcept : winrt::Windows::Management::Setup::IMachineProvisioningProgressReporter(ptr, take_ownership_from_abi) {}
        static auto GetForLaunchUri(winrt::Windows::Foundation::Uri const& launchUri, winrt::Windows::Management::Setup::DeploymentSessionHeartbeatRequested const& heartbeatHandler);
    };
}
#endif
